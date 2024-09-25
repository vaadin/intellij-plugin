package com.vaadin.plugin

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.utils.VaadinProjectUtil.Companion.isVaadinProject

class VaadinProjectDetector : ModuleRootListener, ProjectActivity {

    private val LOG: Logger = Logger.getInstance(VaadinProjectDetector::class.java)

    override fun rootsChanged(event: ModuleRootEvent) {
        if (isVaadinProject(event.project)) {
            doNotifyAboutVaadinProject(event.project)
            LOG.info("Vaadin detected in dependencies of " + event.project.name)
        }
    }

    override suspend fun execute(project: Project) {
        if (isVaadinProject(project)) {
            doNotifyAboutVaadinProject(project)
            LOG.info("Vaadin detected during startup of " + project.name)
        }
    }

    private fun doNotifyAboutVaadinProject(project: Project) {
        val publisher: VaadinProjectListener = project.messageBus.syncPublisher(VaadinProjectListener.TOPIC)
        publisher.vaadinProjectDetected(project)
    }
}
