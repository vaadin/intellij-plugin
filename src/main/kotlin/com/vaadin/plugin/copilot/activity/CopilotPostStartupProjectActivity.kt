package com.vaadin.plugin.copilot.activity

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.PlatformProjectOpenProcessor.Companion.isNewProject
import com.vaadin.plugin.copilot.CopilotPluginUtil

class CopilotPostStartupProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.isNewProject()
        val isVaadinProject = CopilotPluginUtil.isVaadinProject(project)
        if (isVaadinProject) {
            val dotFileDirectory = CopilotPluginUtil.getDotFileDirectory(project)
            if (dotFileDirectory == null) {
                CopilotPluginUtil.createIdeaDirectoryIfMissing(project)
            }
            CopilotPluginUtil.startServer(project)
        }

        ProjectManager.getInstance().addProjectManagerListener(project, object : ProjectManagerListener{
            override fun projectClosing(project: Project) {
                if (CopilotPluginUtil.isServerRunning(project)) {
                    CopilotPluginUtil.stopServer(project)
                }
            }
        })
    }

}
