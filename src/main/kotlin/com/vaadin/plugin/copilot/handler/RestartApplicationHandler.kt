package com.vaadin.plugin.copilot.handler

import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project

class RestartApplicationHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        runInEdt {
            val contentManager = RunContentManager.getInstance(project)
            val selectedDescriptor = contentManager.selectedContent
            if (selectedDescriptor != null) {
                LOG.debug("Restarting ${selectedDescriptor.displayName} (${project.name})")
                ExecutionUtil.restart(selectedDescriptor)
            } else {
                LOG.debug("Restart of ${project.name} failed - content not found")
            }
        }
        return RESPONSE_OK
    }
}
