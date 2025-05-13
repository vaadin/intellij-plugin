package com.vaadin.plugin.hotswapagent

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.openapi.project.Project
import com.vaadin.plugin.utils.VaadinIcons
import javax.swing.Icon

class HotswapAgentExecutor : DefaultDebugExecutor() {

    companion object {
        const val ID = "Vaadin.HotswapAgentExecutor"
    }

    override fun getDescription(): String {
        return "Debug using HotswapAgent"
    }

    override fun getId(): String {
        return ID
    }

    override fun getToolWindowId(): String {
        return id
    }

    override fun getActionName(): String {
        return description
    }

    override fun getStartActionText(): String {
        return description
    }

    override fun getStartActionText(configurationName: String): String {
        return description
    }

    override fun getContextActionId(): String {
        return "$id-action"
    }

    override fun getIcon(): Icon {
        return VaadinIcons.DEBUG_HOTSWAP
    }

    override fun getToolWindowIcon(): Icon {
        return VaadinIcons.DEBUG_HOTSWAP
    }

    override fun getDisabledIcon(): Icon {
        return super.getDisabledIcon()
    }

    override fun isApplicable(project: Project): Boolean {
        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration?.configuration
        return isCurrentFile(selectedConfiguration) ||
            (selectedConfiguration is JavaRunConfigurationBase &&
                (!isMaven(selectedConfiguration) || !isGradle(selectedConfiguration)))
    }

    private fun isCurrentFile(configuration: RunConfiguration?): Boolean {
        return configuration == null
    }

    private fun isMaven(configuration: RunConfiguration): Boolean {
        return configuration.javaClass.name.contains("MavenRunConfiguration")
    }

    private fun isGradle(configuration: RunConfiguration): Boolean {
        return configuration.javaClass.name.contains("GradleRunConfiguration")
    }
}
