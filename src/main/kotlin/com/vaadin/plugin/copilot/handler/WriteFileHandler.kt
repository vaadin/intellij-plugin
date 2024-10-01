package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.Strings
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.vaadin.plugin.actions.VaadinCompileOnSaveAction
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
        val doc = vfsFile.findDocument()
        CommandProcessor.getInstance()
            .executeCommand(
                project,
                {
                    WriteCommandAction.runWriteCommandAction(project) {
                        doWrite(vfsFile, doc, content)
                        commitAndFlush(doc)
                        LOG.info("File $ioFile contents saved")

                        compile(vfsFile)
                        openFileInEditor(vfsFile)
                    }
                },
                undoLabel ?: "Vaadin Copilot Write File",
                if (doc != null) DocCommandGroupId.noneGroupId(doc) else null,
                UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION,
            )
    }

    private fun create() {
        val parentDir = VfsUtil.createDirectories(ioFile.parent)
        val psiDir = runReadAction { PsiManager.getInstance(project).findDirectory(parentDir) }

        if (psiDir != null) {
            runInEdt {
                CommandProcessor.getInstance()
                    .executeCommand(
                        project,
                        {
                            WriteCommandAction.runWriteCommandAction(project) {
                                val psiFile = doCreate(ioFile, content)
                                if (psiFile.containingDirectory == null) {
                                    psiDir.add(psiFile)
                                }

                                VfsUtil.findFileByIoFile(ioFile, true)?.let { vfsFile ->
                                    compile(vfsFile)
                                    openFileInEditor(vfsFile)
                                }
                            }
                            LOG.info("File $ioFile contents saved")
                        },
                        undoLabel ?: "Vaadin Copilot Write File",
                        null,
                        UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION,
                    )
            }
        }
    }

    private fun openFileInEditor(vfsFile: VirtualFile) {
        val openFileDescriptor = OpenFileDescriptor(project, vfsFile)
        FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, false)
    }

    private fun compile(vfsFile: VirtualFile) {
        VaadinCompileOnSaveAction().compile(project, vfsFile)
    }

    open fun doCreate(ioFile: File, content: String): PsiFile {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(ioFile.name)
        return PsiFileFactory.getInstance(project).createFileFromText(ioFile.name, fileType, content)
    }

    open fun doWrite(vfsFile: VirtualFile?, doc: Document?, content: String) {
        doc?.setText(Strings.convertLineSeparators(content))
    }
}
