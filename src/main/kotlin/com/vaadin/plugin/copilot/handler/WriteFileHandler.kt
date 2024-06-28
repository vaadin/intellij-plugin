package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
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
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.task.ProjectTaskManager
import java.io.File

class WriteFileHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val content: String = data["content"] as String
    private val undoLabel: String? = data["undoLabel"] as String?
    private val ioFile: File = File(data["file"] as String)

    override fun run() {
        if (isFileInsideProject(project, ioFile)) {
            // file exists, write content
            val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
            if (vfsFile?.exists() == true) {
                if (ReadonlyStatusHandler.ensureFilesWritable(project, vfsFile)) {
                    writeAndFlush(vfsFile)
                    openFileInEditor(vfsFile)
                    compile(vfsFile)
                } else {
                    LOG.warn("File $ioFile is not writable")
                }
            } else {
                // file does not exist, create new one
                LOG.info("File $ioFile does not exist, creating new file")
                create()?.let {
                    openFileInEditor(it)
                    compile(it)
                }
            }
        } else {
            LOG.warn("File $ioFile is not a part of a project")
        }
    }

    private fun writeAndFlush(vfsFile: VirtualFile) {
        vfsFile.findDocument()?.let {
            CommandProcessor.getInstance().executeCommand(
                project,
                {
                    WriteCommandAction.runWriteCommandAction(project) {
                        it.setText(Strings.convertLineSeparators(content))
                        commitAndFlush(it)
                        LOG.info("File $ioFile contents saved")
                    }
                },
                undoLabel ?: "Vaadin Copilot Write File",
                DocCommandGroupId.noneGroupId(it),
                UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
            )
        }
    }

    private fun openFileInEditor(vfsFile: VirtualFile) {
        val openFileDescriptor = OpenFileDescriptor(project, vfsFile)
        FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, false)
    }

    private fun compile(vfsFile: VirtualFile) {
        ProjectTaskManager.getInstance(project).compile(vfsFile).then {
            LOG.info("File $ioFile compiled")
        }
    }

    private fun create(): VirtualFile? {
        val parentDir = getOrCreateParentDir()
        if (parentDir != null) {
            val parentPsiDir = PsiManager.getInstance(project).findDirectory(parentDir)
            if (parentPsiDir != null) {
                CommandProcessor.getInstance().executeCommand(
                    project,
                    {
                        WriteCommandAction.runWriteCommandAction(project) {
                            val fileType = FileTypeManager.getInstance().getFileTypeByFileName(ioFile.name)
                            val newFile =
                                PsiFileFactory.getInstance(project).createFileFromText(ioFile.name, fileType, content)
                            parentPsiDir.add(newFile)
                        }
                        LOG.info("File $ioFile contents saved")
                    },
                    undoLabel ?: "Vaadin Copilot Write File",
                    DocCommandGroupId.noneGroupId(null),
                    UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
                )
            }
        }
        return VfsUtil.findFileByIoFile(ioFile, true)
    }

    private fun getOrCreateParentDir(): VirtualFile? {
        if (!ioFile.parentFile.exists() && !ioFile.parentFile.mkdirs()) {
            LOG.warn("Cannot create parent directories for ${ioFile.parent}")
            return null
        }
        return VfsUtil.findFileByIoFile(ioFile.parentFile, true)
    }


}
