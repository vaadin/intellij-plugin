package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import io.netty.handler.codec.http.HttpResponseStatus
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

class GetModulePathsHandler(project: Project) : AbstractHandler(project) {

    @JvmRecord
    data class ProjectInfo(
        val basePath: String?,
        val modules: List<ModuleInfo>
    )
    @JvmRecord
    data class ModuleInfo(
        val name: String,
        val contentRoots: Array<String>,
        val javaSourcePaths: Array<String>,
        val javaTestSourcePaths: Array<String>,
        val resourcePaths: Array<String>,
        val testResourcePaths: Array<String>,
        val outputPath:String?
    )

    override fun run(): HandlerResponse {
        val modules = ArrayList<ModuleInfo>()
        ModuleManager.getInstance(project).modules.forEach { module: Module ->
            val moduleRootManager = ModuleRootManager.getInstance(module)
            val contentRoots = moduleRootManager.contentRoots.map { it.path }

            val compilerModuleExtension = CompilerModuleExtension.getInstance(module)

            // Note that the JavaSourceRootType.SOURCE also includes Kotlin source folders
            val javaSourcePaths = moduleRootManager.getSourceRoots(JavaSourceRootType.SOURCE).map { it.path }
            val javaTestSourcePaths = moduleRootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE).map { it.path }

            val resourcePaths = moduleRootManager.getSourceRoots(JavaResourceRootType.RESOURCE).map { it.path }
            val testResourcePaths = moduleRootManager.getSourceRoots(JavaResourceRootType.TEST_RESOURCE).map { it.path }

            val outputPath = compilerModuleExtension?.compilerOutputPath?.path;
            modules.add(
                ModuleInfo(
                    module.name,
                    contentRoots.toTypedArray(),
                    javaSourcePaths.toTypedArray(),
                    javaTestSourcePaths.toTypedArray(),
                    resourcePaths.toTypedArray(),
                    testResourcePaths.toTypedArray(),
                    outputPath ))
        }
        val projectInfo = ProjectInfo(project.guessProjectDir()?.path, modules)
        val data = mapOf("project" to projectInfo)
        return HandlerResponse(HttpResponseStatus.OK, data)
    }
}
