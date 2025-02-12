package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

class DeleteFileHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val ioFile: File = File(data["file"] as String)

    override fun run(): HandlerResponse {
        if (!isFileInsideProject(project, ioFile)) {
            LOG.warn("File $ioFile is not a part of a project")
            return RESPONSE_BAD_REQUEST
        }

        val vfsFile = VfsUtil.findFileByIoFile(ioFile, true) ?: return RESPONSE_ERROR
        if (!vfsFile.exists()) {
            LOG.warn("File $ioFile does not exist")
             return RESPONSE_ERROR
        }

        runInEdt {
            if (!ReadonlyStatusHandler.ensureFilesWritable(project, vfsFile)) {
                LOG.warn("File $ioFile is not writable, cannot delete")
                return@runInEdt
            }

            CommandProcessor.getInstance().executeCommand(
                project,
                {
                    WriteCommandAction.runWriteCommandAction(project) {
                        try {
                            vfsFile.delete(this)
                            LOG.info("File $ioFile deleted")
                        } catch (e: Exception) {
                            LOG.error("Failed to delete file $ioFile", e)
                        }
                    }
                },
                "Vaadin Copilot Delete File",
                null,
                UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
            )
        }

        return RESPONSE_OK
    }
}
