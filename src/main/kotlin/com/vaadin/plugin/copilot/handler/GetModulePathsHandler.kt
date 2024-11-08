package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import io.netty.handler.codec.http.HttpResponseStatus
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

class GetModulePathsHandler(project: Project) : AbstractHandler(project) {

    @JvmRecord data class ProjectInfo(val basePath: String?, val modules: List<ModuleInfo>)

    @JvmRecord
    data class ModuleInfo(
        val name: String,
        val contentRoots: Array<String>,
        val javaSourcePaths: Array<String>,
        val javaTestSourcePaths: Array<String>,
        val resourcePaths: Array<String>,
        val testResourcePaths: Array<String>,
        val outputPath: String?
    )

    override fun run(): HandlerResponse {
        val modules = ArrayList<ModuleInfo>()
        ModuleManager.getInstance(project).modules.forEach { module: Module ->
            val moduleRootManager = ModuleRootManager.getInstance(module)
            val contentRoots = moduleRootManager.contentRoots.map { it.path }

            val compilerModuleExtension = CompilerModuleExtension.getInstance(module)
            val outputPath = compilerModuleExtension?.compilerOutputPath

            // Note that the JavaSourceRootType.SOURCE also includes Kotlin source folders
            // Only include folder if is is not in the output path
            val javaSourcePaths =
                moduleRootManager.getSourceRoots(JavaSourceRootType.SOURCE).filter(exclude(outputPath)).map({ it.path })
            val javaTestSourcePaths =
                moduleRootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE).filter(exclude(outputPath)) map
                    {
                        it.path
                    }

            val resourcePaths =
                moduleRootManager.getSourceRoots(JavaResourceRootType.RESOURCE).filter(exclude(outputPath)).map {
                    it.path
                }
            val testResourcePaths =
                moduleRootManager.getSourceRoots(JavaResourceRootType.TEST_RESOURCE).filter(exclude(outputPath)).map {
                    it.path
                }

            modules.add(
                ModuleInfo(
                    module.name,
                    contentRoots.toTypedArray(),
                    javaSourcePaths.toTypedArray(),
                    javaTestSourcePaths.toTypedArray(),
                    resourcePaths.toTypedArray(),
                    testResourcePaths.toTypedArray(),
                    outputPath?.path))
        }
        val projectInfo = ProjectInfo(project.guessProjectDir()?.path, modules)
        val data = mapOf("project" to projectInfo)
        return HandlerResponse(HttpResponseStatus.OK, data)
    }

    private fun exclude(outputPath: VirtualFile?) = { sourceRoot: VirtualFile ->
        outputPath == null || !VfsUtil.isAncestor(outputPath, sourceRoot, false)
    }
}
