package com.vaadin.plugin.copilot.activity

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.utils.VaadinProjectUtil
import org.jetbrains.ide.BuiltInServerManager

class CopilotPostStartupProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {

        BuiltInServerManager.getInstance().waitForStart()

        if (VaadinProjectUtil.isVaadinProject(project)) {
            val dotFileDirectory =
                CopilotPluginUtil.getDotFileDirectory(project)
            if (dotFileDirectory == null) {
                CopilotPluginUtil.createIdeaDirectoryIfMissing(project)
            }
            CopilotPluginUtil.saveDotFile(project)
        }

        ProjectManager.getInstance()
            .addProjectManagerListener(
                project,
                object : ProjectManagerListener {
                    override fun projectClosing(project: Project) {
                        CopilotPluginUtil.removeDotFile(project)
                    }
                })
    }
}
