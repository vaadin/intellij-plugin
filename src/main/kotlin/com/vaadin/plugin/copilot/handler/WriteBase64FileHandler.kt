package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.io.File
import java.nio.file.Files
import java.util.*
import org.jetbrains.jps.model.java.JavaResourceRootType

class WriteBase64FileHandler(project: Project, data: Map<String, Any>) : WriteFileHandler(project, data) {

    override fun doWrite(vfsFile: VirtualFile?, doc: Document?, content: String) {
        vfsFile!!.setBinaryContent(Base64.getDecoder().decode(content))
        copyResource(vfsFile)
    }

    override fun doCreate(ioFile: File, content: String): PsiFile {
        Files.createFile(ioFile.toPath())
        val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
        doWrite(vfsFile!!, null, content)
        return PsiManager.getInstance(project).findFile(vfsFile)!!
    }

    private fun copyResource(vfsFile: VirtualFile) {
        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vfsFile)!!
        val list = ModuleRootManager.getInstance(module).getSourceRoots(JavaResourceRootType.RESOURCE)
        // find matching resource root for given resource file
        val resourceRoot = list.find { vfsFile.path.startsWith(it.path) }
        val resourceRelativeParentPath = vfsFile.parent.path.substringAfter(resourceRoot!!.path)
        val output = CompilerPaths.getModuleOutputPath(module, false)
        val resourceOutput = VfsUtil.createDirectoryIfMissing(output + resourceRelativeParentPath)
        LOG.info("Copying resource: ${vfsFile.path} to $resourceOutput")
        VfsUtil.copyFile(this, vfsFile, resourceOutput!!)
    }
}
