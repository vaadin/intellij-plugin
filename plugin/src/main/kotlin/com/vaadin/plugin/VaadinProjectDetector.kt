package com.vaadin.plugin

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.concurrency.AppExecutorUtil
import com.vaadin.plugin.copilot.service.CopilotDotfileService
import com.vaadin.plugin.utils.doNotifyAboutVaadinProject
import com.vaadin.plugin.utils.hasVaadin

class VaadinProjectDetector : ModuleRootListener, ProjectActivity, DumbService.DumbModeListener {

    private val LOG: Logger = Logger.getInstance(VaadinProjectDetector::class.java)

    override fun rootsChanged(event: ModuleRootEvent) {
        if (event.project.isOpen) {
            detectAndNotifyAboutVaadinProject(event.project)
        }
    }

    override suspend fun execute(project: Project) {
        detectAndNotifyAboutVaadinProject(project)
    }

    override fun exitDumbMode() {
        ProjectManager.getInstance().openProjects.firstOrNull()?.let { detectAndNotifyAboutVaadinProject(it) }
    }

    private fun detectAndNotifyAboutVaadinProject(project: Project) {
        if (project.isDisposed) {
            return
        }
        // Cancellable read action that yields to pending writes, avoiding EDT freezes (issue #538).
        ReadAction.nonBlocking<Boolean> { !project.service<CopilotDotfileService>().isActive() && hasVaadin(project) }
            .inSmartMode(project)
            .expireWith(project)
            .coalesceBy(VaadinProjectDetector::class.java, project)
            .submit(AppExecutorUtil.getAppExecutorService())
            // Notify off both the EDT and the read lock; the listeners may touch the file system.
            .onSuccess { detected ->
                if (detected && !project.isDisposed) {
                    doNotifyAboutVaadinProject(project)
                    LOG.info("Vaadin project detected " + project.name)
                }
            }
    }
}
