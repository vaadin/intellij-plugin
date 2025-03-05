package com.vaadin.plugin.copilot.handler

import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project

class RestartServiceHandler(project: Project, serviceName: String) : AbstractHandler(project) {

    private val serviceName: String = serviceName as String

    override fun run(): HandlerResponse {
        runInEdt {
            val contentManager = RunContentManager.getInstance(project)
            for (descriptor in contentManager.allDescriptors) {
                if (descriptor.displayName == serviceName) {
                    LOG.debug("Restarting ${descriptor.displayName} (${project.name})")
                    ExecutionUtil.restart(descriptor)
                    break
                }
            }
            LOG.debug("Restart of ${project.name} failed - content not found")
        }
        return RESPONSE_OK
    }
}
