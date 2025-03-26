package com.vaadin.plugin.copilot.handler

import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project

class RestartServiceHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val expectedMainClass: String = data["mainClass"] as String

    override fun run(): HandlerResponse {
        val contentManager = RunContentManager.getInstance(project)
        val allDescriptors = contentManager.allDescriptors
        for (descriptor in allDescriptors) {
            var processHandler = descriptor.processHandler
            if (processHandler is BaseOSProcessHandler) {
                var mainClassName = processHandler.commandLine.split(" ").last()
                if (mainClassName == expectedMainClass) {
                    LOG.debug("Restarting ${descriptor.displayName} (${project.name})")
                    runInEdt { ExecutionUtil.restart(descriptor) }
                    break
                }
            }
        }

        return RESPONSE_OK
    }
}
