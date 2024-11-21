package com.vaadin.plugin.actions

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.dashboard.actions.ExecutorAction
import com.intellij.execution.dashboard.tree.GroupingNode
import com.intellij.execution.dashboard.tree.RunConfigurationNode
import com.intellij.execution.dashboard.tree.RunDashboardGroupImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.vaadin.plugin.hotswapagent.HotswapAgentExecutor
import com.vaadin.plugin.utils.VaadinIcons
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType

class DebugUsingHotSwapAgentAction : ExecutorAction() {
    override fun update(event: AnActionEvent, isRunning: Boolean) {

        if (isRunning) {
            event.presentation.text = "Rerun using HotSwapAgent"
            event.presentation.icon = VaadinIcons.RERUN_HOTSWAP
        } else {
            event.presentation.text = "Debug using HotSwapAgent"
            event.presentation.icon = VaadinIcons.DEBUG_HOTSWAP
        }
    }

    override fun update(e: AnActionEvent) {
        if (e.dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM) is RunConfigurationNode) {
            //            e.presentation.isEnabledAndVisible = false
            if ((e.dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM) as RunConfigurationNode)
                .configurationSettings
                .configuration !is MavenRunConfiguration) {
                //                e.presentation.isEnabledAndVisible = false
                e.presentation.text = "RUN HotSwapAgent"
                return
            } else {
                e.presentation.text = "CANNOT BE RUN HotSwapAgent"
                return
            }
        } else if (e.dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM) is GroupingNode) {
            val runConfigurationType =
                ((e.dataContext.getData(PlatformCoreDataKeys.SELECTED_ITEM) as GroupingNode).group
                        as RunDashboardGroupImpl<*>)
                    .value

            if (runConfigurationType is MavenRunConfigurationType) {
                //                e.presentation.isEnabledAndVisible = false
                e.presentation.text = "CANNOT BE RUN HotSwapAgent"
                return
            }
        }
        e.presentation.text = "Debug using HotSwapAgent"
        e.presentation.icon = VaadinIcons.DEBUG_HOTSWAP
        super.update(e)
    }

    override fun getExecutor(): Executor {
        return ExecutorRegistry.getInstance().getExecutorById(HotswapAgentExecutor.ID)!!
    }
}
