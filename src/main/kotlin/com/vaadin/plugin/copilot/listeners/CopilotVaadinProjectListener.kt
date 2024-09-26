package com.vaadin.plugin.copilot.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.listeners.VaadinProjectListener

class CopilotVaadinProjectListener : VaadinProjectListener {

    private var triggered = false

    override fun vaadinProjectDetected(project: Project) {
        if (!triggered) {
            triggered = true
            createDotFile(project)
            removeDotFileOnExit(project)
        }
    }

    private fun createDotFile(project: Project) {
        val dotFileDirectory = CopilotPluginUtil.getDotFileDirectory(project)
        if (dotFileDirectory == null) {
            CopilotPluginUtil.createIdeaDirectoryIfMissing(project)
        }
        CopilotPluginUtil.saveDotFile(project)
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