package com.vaadin.plugin.copilot.activity

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.copilot.CopilotPluginUtil
import org.jetbrains.ide.BuiltInServerManager

class CopilotPostStartupProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {

        BuiltInServerManager.getInstance().waitForStart()

        val isVaadinProject = CopilotPluginUtil.isVaadinProject(project)
        if (isVaadinProject) {
            val dotFileDirectory = CopilotPluginUtil.getDotFileDirectory(project)
            if (dotFileDirectory == null) {
                CopilotPluginUtil.createIdeaDirectoryIfMissing(project)
            }
            CopilotPluginUtil.startServer(project)
        }

        ProjectManager.getInstance().addProjectManagerListener(project, object : ProjectManagerListener {
            override fun projectClosing(project: Project) {
                if (CopilotPluginUtil.isServerRunning(project)) {
                    CopilotPluginUtil.stopServer(project)
                }
            }
        })
    }

}
