package com.vaadin.plugin.copilot.listeners

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.copilot.service.CopilotDotfileService

class CopilotDynamicPluginListener(private val project: Project) : DynamicPluginListener {

    private val LOG: Logger = Logger.getInstance(CopilotDynamicPluginListener::class.java)

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        project.getService(CopilotDotfileService::class.java).removeDotfile()
        LOG.debug("Plugin is going to be unloaded, .copilot-plugin removed")
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        CopilotPluginUtil.saveDotFile(project)
        LOG.debug("Plugin loaded, .copilot-plugin created")
    }
}
