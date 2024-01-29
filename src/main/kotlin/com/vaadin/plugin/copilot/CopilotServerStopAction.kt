package com.vaadin.plugin.copilot

import com.intellij.ide.AppLifecycleListener
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class CopilotServerStopAction: AnAction(), AppLifecycleListener {

    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project
        if (project == null) {
            notify("Copilot Plugin can be started only on existing project", NotificationType.WARNING)
            return
        }

        val server = p0.project?.service<CopilotServerService>()
        if (server?.isRunning() == false) {
            notify("Copilot Plugin is not active", NotificationType.INFORMATION)
            return
        }

        server?.stop()
        notify("Copilot Plugin stopped", NotificationType.INFORMATION)
    }

    private fun notify(message: String, type: NotificationType) {
        Notifications.Bus.notify(Notification("Copilot", message, type))
    }

}