package com.vaadin.plugin.copilot

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CopilotPostStartupProjectActivity: ProjectActivity {

    override suspend fun execute(project: Project) {
        val isVaadinProject = CopilotPluginUtil.isVaadinProject(project)
        if (isVaadinProject) {
            CopilotPluginUtil.startServer(project)
        }
    }

}