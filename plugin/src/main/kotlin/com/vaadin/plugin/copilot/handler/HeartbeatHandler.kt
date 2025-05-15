package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.service.CompilationStatusManagerService
import io.netty.handler.codec.http.HttpResponseStatus

class HeartbeatHandler(project: Project) : AbstractHandler(project) {
    companion object {
        const val HAS_COMPILATION_ERROR = "hasCompilationError"
        const val FILES_CONTAIN_COMPILATION_ERROR = "filesContainCompilationError"
    }

    override fun run(): HandlerResponse {
        val data: MutableMap<String, Any> = mutableMapOf()
        val compilationStatusManagerService = project.getService(CompilationStatusManagerService::class.java)
        data[HAS_COMPILATION_ERROR] = compilationStatusManagerService.hasCompilationError()
        data[FILES_CONTAIN_COMPILATION_ERROR] = compilationStatusManagerService.getErrorFiles()
        return HandlerResponse(HttpResponseStatus.OK, data)
    }
}
