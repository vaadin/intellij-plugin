package com.vaadin.plugin.copilot.listeners

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.saveDotFile
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.ui.VaadinStatusBarWidget

class CopilotVaadinProjectListener : VaadinProjectListener {

    private var triggered = false

    override fun vaadinProjectDetected(project: Project) {
        if (!triggered && !project.isDisposed) {
            triggered = true
            saveDotFile(project)
            removeDotFileOnExit(project)
            DumbService.getInstance(project).smartInvokeLater { VaadinStatusBarWidget.update(project) }
        }
    }

    private fun removeDotFileOnExit(project: Project) {
        ProjectManager.getInstance()
            .addProjectManagerListener(
                project,
                object : ProjectManagerListener {
                    override fun projectClosing(project: Project) {
                        CopilotPluginUtil.removeDotFile(project)
                    }
                },
            )
    }
}
