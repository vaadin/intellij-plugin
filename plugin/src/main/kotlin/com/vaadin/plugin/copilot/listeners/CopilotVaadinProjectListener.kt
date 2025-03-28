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

    private var listenerRegistered = false

    override fun vaadinProjectDetected(project: Project) {
        if (!project.isDisposed) {
            saveDotFile(project)
            removeDotFileOnExit(project)
            DumbService.getInstance(project).smartInvokeLater { VaadinStatusBarWidget.update(project) }
        }
    }

    private fun removeDotFileOnExit(project: Project) {
        if (!listenerRegistered) {
            listenerRegistered = true
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
}
