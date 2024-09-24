package com.vaadin.plugin.copilot.activity

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.copilot.CopilotPluginUtil

class CopilotPostStartupProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
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
