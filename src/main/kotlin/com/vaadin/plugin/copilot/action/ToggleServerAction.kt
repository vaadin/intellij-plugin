package com.vaadin.plugin.copilot.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.vaadin.plugin.copilot.CopilotPluginUtil

class ToggleServerAction : AnAction() {

    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project ?: return

        if (CopilotPluginUtil.isServerRunning(project)) {
            CopilotPluginUtil.stopServer(project)
        } else {
            CopilotPluginUtil.startServer(project)
        }

    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        if (project == null || !CopilotPluginUtil.isVaadinProject(project)) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        if (CopilotPluginUtil.isServerRunning(project)) {
            e.presentation.text = "Stop Vaadin Copilot Integration"
        } else {
            e.presentation.text = "Start Vaadin Copilot Integration"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}
