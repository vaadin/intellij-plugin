package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findDocument
import com.vaadin.plugin.actions.VaadinCompileOnSaveActionInfo
import java.io.File

class CompileFilesHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val ioFile = data["files"] as Collection<String>

    override fun run(): HandlerResponse {

        val documents =
            ioFile
                .map { File(it) }
                .filter { isFileInsideProject(project, it) }
                .mapNotNull { VfsUtil.findFileByIoFile(it, true) }
                .map { it.findDocument() }
                .toTypedArray()

        if (documents.isNotEmpty()) {
            VaadinCompileOnSaveActionInfo.getAction().processDocuments(project, documents)
            LOG.debug("Files compilation initiated: $documents")
            return RESPONSE_OK
        } else {
            LOG.warn("No project files found in $ioFile")
            return RESPONSE_BAD_REQUEST
        }
    }
}
