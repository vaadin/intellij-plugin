package com.vaadin.plugin.copilot.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.downloadLatestCopilotChatJar
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.downloadLatestCopilotLocalMcpServerJar
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.existsLatestCopilotChatJar
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.existsLatestCopilotLocalMcpServerJar
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.saveDotFile
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.startChatApp
import com.vaadin.plugin.copilot.service.CopilotDotfileService
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.ui.VaadinStatusBarWidget

class CopilotVaadinProjectListener : VaadinProjectListener {

    override fun vaadinProjectDetected(project: Project) {
        if (!project.isDisposed && !project.service<CopilotDotfileService>().isActive()) {
            saveDotFile(project)
            if (existsLatestCopilotChatJar() && existsLatestCopilotLocalMcpServerJar()) {
                startChatApp(project)
            } else {
                when {
                    !existsLatestCopilotChatJar() && !existsLatestCopilotLocalMcpServerJar() -> {
                        // Download latest chat jar
                        downloadLatestCopilotChatJar(project).thenRun {
                            downloadLatestCopilotLocalMcpServerJar(project).thenRun { startChatApp(project) }
                        }
                    }
                    existsLatestCopilotChatJar() && !existsLatestCopilotLocalMcpServerJar() -> {
                        // Download latest local MCP server jar
                        downloadLatestCopilotLocalMcpServerJar(project).thenRun { startChatApp(project) }
                    }
                    !existsLatestCopilotChatJar() && existsLatestCopilotLocalMcpServerJar() -> {
                        // Download latest chat jar
                        downloadLatestCopilotChatJar(project).thenRun { startChatApp(project) }
                    }
                }
            }
            DumbService.getInstance(project).smartInvokeLater { VaadinStatusBarWidget.update(project) }
        }
    }
}
