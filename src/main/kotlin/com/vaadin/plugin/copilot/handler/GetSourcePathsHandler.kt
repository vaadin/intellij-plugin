package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import io.netty.handler.codec.http.HttpResponseStatus
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

class GetSourcePathsHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        val sourcePaths = ArrayList<String>()
        val testSourcePaths = ArrayList<String>()
        val resourcePaths = ArrayList<String>()
        val testResourcePaths = ArrayList<String>()
        ModuleManager.getInstance(project).modules.forEach { module: Module ->
            val moduleRootManager = ModuleRootManager.getInstance(module)
            sourcePaths.addAll(moduleRootManager.getSourceRoots(JavaSourceRootType.SOURCE).map { it.path })
            testSourcePaths.addAll(moduleRootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE).map { it.path })
            resourcePaths.addAll(moduleRootManager.getSourceRoots(JavaResourceRootType.RESOURCE).map { it.path })
            testResourcePaths.addAll(
                moduleRootManager.getSourceRoots(JavaResourceRootType.TEST_RESOURCE).map { it.path })
        }
        val data: Map<String, Any> =
            mapOf(
                "sourcePaths" to sourcePaths,
                "testSourcePaths" to testSourcePaths,
                "resourcePaths" to resourcePaths,
                "testResourcePaths" to testResourcePaths)
        return HandlerResponse(HttpResponseStatus.OK, data)
    }
}
