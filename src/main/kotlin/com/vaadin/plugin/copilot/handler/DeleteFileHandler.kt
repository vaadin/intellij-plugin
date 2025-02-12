package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

class DeleteFileHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val ioFile: File = File(data["file"] as String)

    override fun run() {
        if (isFileInsideProject(project, ioFile)) {
            val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
            if (vfsFile?.exists() == true) {
                ApplicationManager.getApplication().runWriteAction {
                    vfsFile.delete(this)
                    LOG.info("File $ioFile deleted")
                }
            } else {
                LOG.warn("File $ioFile does not exist")
            }
        } else {
            LOG.warn("File $ioFile is not a part of a project")
        }
    }
}