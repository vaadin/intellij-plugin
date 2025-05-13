package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.CompilationStatusManager
import io.netty.handler.codec.http.HttpResponseStatus

class HeartbeatHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {
    companion object {
        const val HAS_COMPILATION_ERROR = "hasCompilationError"
        const val FILES_CONTAIN_COMPILATION_ERROR = "filesContainCompilationError"
    }

    override fun run(): HandlerResponse {
        val data: MutableMap<String, Any> = mutableMapOf()
        data[HAS_COMPILATION_ERROR] = CompilationStatusManager.hasCompilationError(project)
        data[FILES_CONTAIN_COMPILATION_ERROR] = CompilationStatusManager.getErrorFiles(project)
        return HandlerResponse(HttpResponseStatus.OK, data)
    }
}
