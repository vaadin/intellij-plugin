package com.vaadin.plugin.copilot.listener

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseListener
import com.vaadin.plugin.copilot.CopilotPluginUtil

class CopilotProjectLifecycleListener: ProjectCloseListener {

    override fun projectClosing(project: Project) {
        if (CopilotPluginUtil.isServerRunning(project)) {
            CopilotPluginUtil.stopServer(project)
        }
    }

}
