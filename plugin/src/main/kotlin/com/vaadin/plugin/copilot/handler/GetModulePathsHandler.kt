package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.getModulesInfo
import io.netty.handler.codec.http.HttpResponseStatus

class GetModulePathsHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        return ApplicationManager.getApplication().runReadAction<HandlerResponse> {
            val modules = getModulesInfo(project)
            val projectInfo = CopilotPluginUtil.ProjectInfo(project.guessProjectDir()?.path, modules)
            val data = mapOf("project" to projectInfo)
            HandlerResponse(HttpResponseStatus.OK, data)
        }
    }
}
