package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File
import java.nio.file.Files
import java.util.*

class WriteBinaryFileHandler(project: Project, data: Map<String, Any>) : WriteFileHandler(project, data) {

    override fun doWrite(vfsFile: VirtualFile?, doc: Document?, content: String) {
        vfsFile?.let {
            it.setBinaryContent(Base64.getDecoder().decode(content))
        }
    }

    override fun doCreate(ioFile: File, content: String): PsiFile {
        Files.createFile(ioFile.toPath())
        val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
        doWrite(vfsFile!!, null, content)
        return PsiManager.getInstance(project).findFile(vfsFile)!!
    }
}
