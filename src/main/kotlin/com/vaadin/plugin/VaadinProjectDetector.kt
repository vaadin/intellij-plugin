package com.vaadin.plugin

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.utils.VaadinProjectUtil
import com.vaadin.plugin.utils.VaadinProjectUtil.Companion.findVaadinModule

class VaadinProjectDetector : ModuleRootListener, ProjectActivity {

    private val LOG: Logger = Logger.getInstance(VaadinProjectDetector::class.java)

    override fun rootsChanged(event: ModuleRootEvent) {
        if (event.project.isOpen) {
            detectVaadinAndNotify(event.project)
        }
    }

    override suspend fun execute(project: Project) {
        detectVaadinAndNotify(project)
    }

    private fun detectVaadinAndNotify(project: Project) {
        findVaadinModule(project)?.let { module ->
            project.putUserData(
                VaadinProjectUtil.VAADIN_MODULE_ROOTS, ModuleRootManager.getInstance(module).contentRoots)
            project.messageBus.syncPublisher(VaadinProjectListener.TOPIC).vaadinProjectDetected(project)
            LOG.info("Detected Vaadin module: ${module.name}")
        }
    }
}
