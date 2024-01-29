package com.vaadin.plugin.copilot

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseListener

class CopilotProjectLifecycleListener: ProjectCloseListener {

    override fun projectClosing(project: Project) {
        super.projectClosing(project)
        val server = project.service<CopilotServerService>()
        if (server.isRunning()) {
            server.stop()
        }
    }

}