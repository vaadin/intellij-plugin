package com.vaadin.plugin.copilot.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.removeDotFile
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.saveDotFile
import com.vaadin.plugin.listeners.VaadinProjectListener

class CopilotVaadinProjectListener : VaadinProjectListener {

    private var triggered = false

    override fun vaadinProjectDetected(project: Project) {
        if (!triggered) {
            triggered = true
            saveDotFile(project)
            removeDotFileOnExit(project)
        }
    }

    private fun removeDotFileOnExit(project: Project) {
        ProjectManager.getInstance()
            .addProjectManagerListener(
                project,
                object : ProjectManagerListener {
                    override fun projectClosing(project: Project) {
                        removeDotFile(project)
                    }
                },
            )
    }
}
