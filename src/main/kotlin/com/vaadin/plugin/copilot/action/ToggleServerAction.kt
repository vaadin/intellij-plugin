package com.vaadin.plugin.copilot.action

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.vaadin.plugin.copilot.CopilotPluginUtil

class ToggleServerAction : AnAction() {

    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project
        if (project == null) {
            CopilotPluginUtil.notify("Copilot Plugin can be started only on existing project", NotificationType.WARNING)
            return
        }

        if (CopilotPluginUtil.isServerRunning(project)) {
            CopilotPluginUtil.stopServer(project)
        } else {
            CopilotPluginUtil.startServer(project)
        }

    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY)?.first() is PsiDirectoryNode

        val project = e.project
        if (project != null && CopilotPluginUtil.isServerRunning(project)) {
            e.presentation.text = "Stop Copilot Plugin"
        } else {
            e.presentation.text = "Start Copilot Plugin"
        }
    }

}
