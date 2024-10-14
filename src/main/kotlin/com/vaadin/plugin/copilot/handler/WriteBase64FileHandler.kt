package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Base64

class WriteBase64FileHandler(project: Project, data: Map<String, Any>) : WriteFileHandler(project, data) {

    override fun doWrite(vfsFile: VirtualFile?, doc: Document?, content: String) {
        vfsFile?.setBinaryContent(Base64.getDecoder().decode(content))
    }
}
