package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.vaadin.plugin.utils.VAADIN_SERVICE
import io.netty.handler.codec.http.HttpResponseStatus

class GetVaadinVersionHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        return ApplicationManager.getApplication().runReadAction<HandlerResponse> {
            val psiClass: PsiClass =
                JavaPsiFacade.getInstance(project).findClass(VAADIN_SERVICE, GlobalSearchScope.allScope(project))
                    ?: return@runReadAction HandlerResponse(
                        status = HttpResponseStatus.NOT_FOUND, data = mapOf("error" to "VaadinService class not found"))

            LOG.info("Vaadin Service psiClass: ${psiClass.qualifiedName}")

            val classFile: VirtualFile =
                psiClass.containingFile.virtualFile
                    ?: return@runReadAction HandlerResponse(
                        status = HttpResponseStatus.NOT_FOUND,
                        data = mapOf("error" to "VaadinService class file not found"))

            LOG.info("Vaadin Service class file: ${classFile.path}")

            val jarFile = JarFileSystem.getInstance().getVirtualFileForJar(classFile)
            val version =
                if (jarFile != null) {
                    extractArtifactVersionFromJarPath(jarFile.path)
                } else {
                    "N/A"
                }

            LOG.info("Vaadin Version detected: $version")

            HandlerResponse(status = HttpResponseStatus.OK, data = mapOf("version" to version) as Map<String, Any>?)
        }
    }

    fun extractArtifactVersionFromJarPath(jarPath: String): String? {
        val jarName = jarPath.substringAfterLast('/')
        if (!jarName.endsWith(".jar")) return null

        val baseName = jarName.removeSuffix(".jar")

        // Heuristic: version starts at the last dash, assuming format like artifactId-version.jar
        val dashIndex = baseName.lastIndexOf('-')
        if (dashIndex == -1 || dashIndex == baseName.length - 1) return null

        return baseName.substring(dashIndex + 1)
    }
}
