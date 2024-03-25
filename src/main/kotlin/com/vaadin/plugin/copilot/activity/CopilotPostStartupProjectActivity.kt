package com.vaadin.plugin.copilot.activity

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.copilot.CopilotPluginUtil

class CopilotPostStartupProjectActivity: ProjectActivity {

    override suspend fun execute(project: Project) {
        val isVaadinProject = CopilotPluginUtil.isVaadinProject(project)
        val dotFileDirectory = CopilotPluginUtil.getDotFileDirectory(project)
        if (dotFileDirectory == null) {
            CopilotPluginUtil.notifyForDotDirCreation(project)
        } else if (isVaadinProject) {
            CopilotPluginUtil.startServer(project)
        }
    }

}
