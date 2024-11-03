package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import io.netty.handler.codec.http.HttpResponseStatus
import org.jetbrains.jps.model.java.JavaSourceRootType

class GetSourcePathsHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        val sourcePaths = ArrayList<String>()
        val testPaths = ArrayList<String>()
        ModuleManager.getInstance(project).modules.forEach { module: Module ->
            val moduleRootManager = ModuleRootManager.getInstance(module)
            sourcePaths.addAll(moduleRootManager.getSourceRoots(JavaSourceRootType.SOURCE).map { it.path })
            testPaths.addAll(moduleRootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE).map { it.path })
        }
        val data: Map<String, Any> = mapOf("sourcePaths" to sourcePaths, "testSourcePaths" to testPaths)
        return HandlerResponse(HttpResponseStatus.OK, data)
    }
}
