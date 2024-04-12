package com.vaadin.plugin.copilot

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class CopilotStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String {
        return "CopilotStatusBarWidgetFactory"
    }

    override fun getDisplayName(): String {
        return "Vaadin"
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return CopilotStatusBarPanel(project)
    }
}