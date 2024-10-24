package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.Strings
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.vaadin.plugin.utils.IdeUtil
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.File

open class WriteFileHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val content: String = data["content"] as String
    private val undoLabel: String? = data["undoLabel"] as String?
    private val ioFile: File = File(data["file"] as String)

    override fun run(): HandlerResponse {
        var response = RESPONSE_OK
        if (isFileInsideProject(project, ioFile)) {
            // file exists, write content
            val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
            if (vfsFile?.exists() == true) {
                runInEdt {
                    if (ReadonlyStatusHandler.ensureFilesWritable(project, vfsFile)) {
                        writeAndFlush(vfsFile)
                    } else {
                        LOG.warn("File $ioFile is not writable")
                    }
                }
            } else {
                // file does not exist, create new one
                LOG.info("File $ioFile does not exist, creating new file")
                create()
                if (IdeUtil.willVcsPopupBeShown(project)) {
                    IdeUtil.bringToFront(project)
                    response = HandlerResponse(HttpResponseStatus.OK, mapOf("blockingPopup" to "true"))
                }
            }

            return response
        } else {
            LOG.warn("File $ioFile is not a part of a project")
            return RESPONSE_BAD_REQUEST
        }
    }

    private fun writeAndFlush(vfsFile: VirtualFile) {
        vfsFile.findDocument()?.let {
            CommandProcessor.getInstance()
                .executeCommand(
                    project,
                    {
                        WriteCommandAction.runWriteCommandAction(project) {
                            doWrite(vfsFile, it, content)
                            postSave(vfsFile)
                        }
                    },
                    undoLabel ?: "Vaadin Copilot Write File",
                    DocCommandGroupId.noneGroupId(it),
                    UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION,
                )
        }
    }

    private fun create() {
        runInEdt {
            CommandProcessor.getInstance()
                .executeCommand(
                    project,
                    {
                        WriteCommandAction.runWriteCommandAction(project) {
                            val vfsFile = doCreate(ioFile, content)
                            postSave(vfsFile)
                        }
                    },
                    undoLabel ?: "Vaadin Copilot Write File",
                    null,
                    UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION,
                )
        }
    }

    open fun doCreate(file: File, content: String): VirtualFile {
        val parent = VfsUtil.createDirectories(file.parent)
        val vfsFile = parent.createChildData(this, file.name)
        doWrite(vfsFile, vfsFile.findDocument(), content)
        return vfsFile
    }

    open fun doWrite(vfsFile: VirtualFile?, doc: Document?, content: String) {
        doc?.setText(Strings.convertLineSeparators(content))
    }
}
