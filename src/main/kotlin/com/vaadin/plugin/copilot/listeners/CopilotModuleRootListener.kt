package com.vaadin.plugin.copilot.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.utils.VaadinProjectUtil.Companion.isVaadinProject

class CopilotModuleRootListener : ModuleRootListener {

    override fun rootsChanged(event: ModuleRootEvent) {
        if (isVaadinProject(event.project)) {
            createDotFile(event.project)
        }
    }

    private fun createDotFile(project: Project) {
        val dotFileDirectory = CopilotPluginUtil.getDotFileDirectory(project)
        if (dotFileDirectory == null) {
            CopilotPluginUtil.createIdeaDirectoryIfMissing(project)
        }
        CopilotPluginUtil.saveDotFile(project)
    }
}
