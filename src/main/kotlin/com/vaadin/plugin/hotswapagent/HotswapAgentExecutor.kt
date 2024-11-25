package com.vaadin.plugin.hotswapagent

import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.openapi.project.Project
import com.vaadin.plugin.utils.VaadinIcons
import javax.swing.Icon
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class HotswapAgentExecutor : DefaultDebugExecutor() {

    companion object {
        val ID = "Vaadin.HotswapAgentExecutor"
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
        return selectedConfiguration is JavaRunConfigurationBase &&
            (selectedConfiguration !is MavenRunConfiguration || selectedConfiguration !is GradleRunConfiguration)
    }
}
