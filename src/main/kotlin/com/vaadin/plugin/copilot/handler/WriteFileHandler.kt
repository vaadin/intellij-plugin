package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import java.io.File

class WriteFileHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val content: String = data["content"] as String
    private val undoLabel: String? = data["undoLabel"] as String?
    private val ioFile: File = File(data["file"] as String)
    private val vfsFile: VirtualFile?

    init {
        if (isFileInsideProject(project, ioFile)) {
            vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
        } else {
            LOG.warn("File $ioFile is not a part of a project")
            vfsFile = null
        }
    }

    override fun run() {
        // file exists, write content
        if (vfsFile?.exists() == true) {
            if (ReadonlyStatusHandler.ensureFilesWritable(project, vfsFile)) {
                writeAndFlush()
            } else {
                LOG.warn("File ${vfsFile.name} is not writable")
            }
        } else {
            // file does not exist, create new one
            LOG.info("File $ioFile does not exist, creating new file")
            create()
        }
    }

    private fun writeAndFlush() {
        vfsFile?.findDocument()?.let {
            CommandProcessor.getInstance().executeCommand(
                project,
                {
                    WriteCommandAction.runWriteCommandAction(project) {
                        it.setText(content)
                        commitAndFlush(it)
                        LOG.info("File ${vfsFile.name} contents saved")
                    }
                },
                undoLabel ?: "Copilot Write File",
                DocCommandGroupId.noneGroupId(it),
                UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
            )
        }
    }

    private fun create() {
        VfsUtil.findFileByIoFile(ioFile.parentFile, true)?.let {
            PsiManager.getInstance(project).findDirectory(it)?.let {it2 ->
                ApplicationManager.getApplication().runWriteAction {
                    val fileType = FileTypeManager.getInstance().getFileTypeByFileName(ioFile.name)
                    val newFile = PsiFileFactory.getInstance(project).createFileFromText(ioFile.name, fileType, content)
                    it2.add(newFile)
                }
                VfsUtil.findFileByIoFile(ioFile, true)
                LOG.info("File ${ioFile.name} contents saved")
            }
        }
    }


}
