package com.vaadin.plugin.copilot.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.saveDotFile
import com.vaadin.plugin.copilot.service.CopilotDotfileService
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.ui.VaadinStatusBarWidget

class CopilotVaadinProjectListener : VaadinProjectListener {

    override fun vaadinProjectDetected(project: Project) {
        if (!project.isDisposed && !project.service<CopilotDotfileService>().isActive()) {
            saveDotFile(project)
            DumbService.getInstance(project).smartInvokeLater { VaadinStatusBarWidget.update(project) }
        }
    }
}
