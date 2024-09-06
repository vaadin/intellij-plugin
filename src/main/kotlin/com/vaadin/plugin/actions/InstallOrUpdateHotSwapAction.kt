package com.vaadin.plugin.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.utils.VaadinHomeUtil

class InstallOrUpdateHotSwapAction : AnAction() {

    override fun actionPerformed(p0: AnActionEvent) {
        val version = VaadinHomeUtil.updateOrInstallHotSwapJar()
        if (version != null) {
            CopilotPluginUtil.notify("hotswap-agent.jar:$version installed", NotificationType.INFORMATION, p0.project)
        } else {
            CopilotPluginUtil.notify(
                "Installation of hotswap-agent.jar failed, see logs for details",
                NotificationType.ERROR,
                p0.project
            )
        }
    }

}
