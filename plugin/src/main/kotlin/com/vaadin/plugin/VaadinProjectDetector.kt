package com.vaadin.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.utils.doNotifyAboutVaadinProject
import com.vaadin.plugin.utils.hasVaadin

class VaadinProjectDetector : ModuleRootListener, ProjectActivity, DumbService.DumbModeListener {

    private val LOG: Logger = Logger.getInstance(VaadinProjectDetector::class.java)

    override fun rootsChanged(event: ModuleRootEvent) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ReadAction.run<Throwable> {
                if (event.project.isOpen && hasVaadin(event.project)) {
                    doNotifyAboutVaadinProject(event.project)
                    LOG.info("Vaadin detected in dependencies of " + event.project.name)
                }
            }
        }
    }

    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ReadAction.run<Throwable> {
                if (hasVaadin(project)) {
                    doNotifyAboutVaadinProject(project)
                    LOG.info("Vaadin detected during startup of " + project.name)
                }
            }
        }
    }

    override fun exitDumbMode() {
        ApplicationManager.getApplication().executeOnPooledThread {
            ReadAction.run<Throwable> {
                val project = ProjectManager.getInstance().openProjects.first()
                if (hasVaadin(project)) {
                    doNotifyAboutVaadinProject(project)
                    LOG.info("Vaadin detected after dumb mode exited " + project.name)
                }
            }
        }
    }
}
