package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import io.netty.handler.codec.http.HttpResponseStatus

internal const val VAADIN_SERVICE = "com.vaadin.flow.server.VaadinService"

class GetVaadinVersionHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        val psiClass: PsiClass =
            JavaPsiFacade.getInstance(project).findClass(VAADIN_SERVICE, GlobalSearchScope.allScope(project))
                ?: return HandlerResponse(
                    status = HttpResponseStatus.NOT_FOUND, data = mapOf("error" to "VaadinService class not found"))

        val classFile: VirtualFile =
            psiClass.containingFile.virtualFile
                ?: return HandlerResponse(
                    status = HttpResponseStatus.NOT_FOUND, data = mapOf("error" to "VaadinService class not found"))

        // If class file is inside a JAR, this returns the jar root
        val jarFile = JarFileSystem.getInstance().getVirtualFileForJar(classFile)
        val version =
            if (jarFile != null) {
                // Extract version from the JAR file name
                extractArtifactVersionFromJarPath(jarFile.path)
            } else {
                // If not in a JAR, we cannot determine the version
                "N/A"
            }
        return HandlerResponse(status = HttpResponseStatus.OK, data = mapOf("version" to version) as Map<String, Any>?)
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
