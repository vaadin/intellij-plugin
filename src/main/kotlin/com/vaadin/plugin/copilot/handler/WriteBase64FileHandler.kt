package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.ProjectTaskManager
import java.util.Base64

class WriteBase64FileHandler(project: Project, data: Map<String, Any>) : WriteFileHandler(project, data) {

    override fun doWrite(vfsFile: VirtualFile?, doc: Document?, content: String) {
        vfsFile?.setBinaryContent(Base64.getDecoder().decode(content))
    }

    override fun postSave(vfsFile: VirtualFile) {
        LOG.info("File $vfsFile created")
        notifyUndoManager(vfsFile)
        // there is no Document associated with binary VirtualFile, call "compile" always to process resource
        processResource(vfsFile)
        openFileInEditor(vfsFile)
    }

    private fun processResource(vfsFile: VirtualFile) {
        ProjectTaskManager.getInstance(project).compile(vfsFile).then {
            if (it.hasErrors()) {
                LOG.warn("Cannot process $vfsFile")
            }
        }
    }
}
