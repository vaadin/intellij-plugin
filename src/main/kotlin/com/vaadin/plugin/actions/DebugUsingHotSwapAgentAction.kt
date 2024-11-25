package com.vaadin.plugin.actions

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.dashboard.actions.ExecutorAction
import com.intellij.execution.dashboard.tree.RunConfigurationNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.vaadin.plugin.hotswapagent.HotswapAgentExecutor
import com.vaadin.plugin.utils.VaadinIcons
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class DebugUsingHotSwapAgentAction : ExecutorAction() {
    override fun update(event: AnActionEvent, isRunning: Boolean) {
        if (event.dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM) != null) {
            if (event.dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM) is RunConfigurationNode) {
                val runConfiguration =
                    (event.dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM) as RunConfigurationNode)
                        .configurationSettings
                        .configuration
                if (runConfiguration is MavenRunConfiguration || runConfiguration is GradleRunConfiguration) {
                    event.presentation.isVisible = false
                    event.presentation.text = null
                    return
                }
            }
        }
        if (isRunning) {
            event.presentation.text = "Rerun using HotSwapAgent"
            event.presentation.icon = VaadinIcons.RERUN_HOTSWAP
        } else {
            event.presentation.text = "Debug using HotSwapAgent"
            event.presentation.icon = VaadinIcons.DEBUG_HOTSWAP
        }
    }

    override fun getExecutor(): Executor {
        return ExecutorRegistry.getInstance().getExecutorById(HotswapAgentExecutor.ID)!!
    }
}
