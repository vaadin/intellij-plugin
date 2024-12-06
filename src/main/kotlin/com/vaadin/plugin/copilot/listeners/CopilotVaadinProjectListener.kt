package com.vaadin.plugin.copilot.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.wm.WindowManager
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.saveDotFile
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.ui.VaadinStatusBarWidget

class CopilotVaadinProjectListener : VaadinProjectListener {

    private var triggered = false

    override fun vaadinProjectDetected(project: Project) {
        if (!triggered) {
            triggered = true
            saveDotFile(project)
            removeDotFileOnExit(project)
            updateStatusBarWidget(project)
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

    private fun updateStatusBarWidget(project: Project) {
        WindowManager.getInstance().getStatusBar(project).updateWidget(VaadinStatusBarWidget.ID)
    }
}
