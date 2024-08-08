package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class MoveFileHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val source: File = File(data["source"] as String)
    private val target: File = File(data["target"] as String)

    override fun run() {
        if (isFileInsideProject(project, target)) {
            getOrCreateParentDir()?.let {
                PsiManager.getInstance(project).findDirectory(it)?.let { it2 ->
                    ApplicationManager.getApplication().runWriteAction {
                        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        val vfsFile = VfsUtil.findFileByIoFile(target, true)!!
                        val psiFile = PsiManager.getInstance(project).findFile(vfsFile)!!
                        if (psiFile.containingDirectory == null) {
                            it2.add(psiFile)
                        }
                    }
                    LOG.info("File $target saved")
                }
            }
        }
    }

    private fun getOrCreateParentDir(): VirtualFile? {
        if (!target.parentFile.exists() && !target.parentFile.mkdirs()) {
            LOG.warn("Cannot create parent directories for ${target.parent}")
            return null
        }
        return VfsUtil.findFileByIoFile(target.parentFile, true)
    }

}
