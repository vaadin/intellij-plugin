package com.vaadin.plugin.copilot.handler

import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager

class ReloadMavenModuleHandler(project: Project, moduleName: String) : AbstractHandler(project) {

    private val moduleName: String = moduleName as String

    override fun run(): HandlerResponse {
        runInEdt {
            val mavenProjectsManager = MavenProjectsManager.getInstance(project)
            mavenProjectsManager.projects.firstOrNull { it.displayName == moduleName }?.let { mavenProject ->
                LOG.debug("Reloading ${mavenProject.displayName} (${project.name})")
                mavenProjectsManager.scheduleForceUpdateMavenProject(mavenProject)
                return@runInEdt
            }
            LOG.debug("Reloading of $moduleName failed - content not found")
        }
        return RESPONSE_OK
    }
}
