package com.vaadin.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.copilot.service.CopilotDotfileService
import com.vaadin.plugin.utils.doNotifyAboutVaadinProject
import com.vaadin.plugin.utils.hasVaadin

class VaadinProjectDetector : ModuleRootListener, ProjectActivity, DumbService.DumbModeListener {

    private val LOG: Logger = Logger.getInstance(VaadinProjectDetector::class.java)

    override fun rootsChanged(event: ModuleRootEvent) {
        if (event.project.isOpen) {
            detectAndNotifyAboutVaadinProject { event.project }
        }
    }

    override suspend fun execute(project: Project) {
        detectAndNotifyAboutVaadinProject { project }
    }

    override fun exitDumbMode() {
        detectAndNotifyAboutVaadinProject { ProjectManager.getInstance().openProjects.first() }
    }

    private fun detectAndNotifyAboutVaadinProject(projectProvider: () -> Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ReadAction.run<Throwable> {
                val project = projectProvider.invoke()
                if (!project.service<CopilotDotfileService>().isActive() && hasVaadin(project)) {
                    doNotifyAboutVaadinProject(project)
                    LOG.info("Vaadin project detected " + project.name)
                }
            }
        }
    }
}
