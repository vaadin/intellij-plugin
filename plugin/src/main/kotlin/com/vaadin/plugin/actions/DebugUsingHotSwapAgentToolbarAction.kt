package com.vaadin.plugin.actions

import com.intellij.execution.ExecutionManager
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionIdProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.vaadin.plugin.hotswapagent.HotswapAgentExecutor
import com.vaadin.plugin.utils.VaadinIcons

class DebugUsingHotSwapAgentToolbarAction : AnAction(), DumbAware, ActionIdProvider {

    override fun getId(): String = HotswapAgentExecutor.ID

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val executor = ExecutorRegistry.getInstance().getExecutorById(HotswapAgentExecutor.ID)
        if (project == null || executor == null || !executor.isApplicable(project)) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        e.presentation.isEnabledAndVisible = true
        val selected = RunManager.getInstance(project).selectedConfiguration
        val running =
            selected != null &&
                ExecutionManager.getInstance(project).getRunningDescriptors { it === selected }.isNotEmpty()
        if (running) {
            e.presentation.text = "Rerun using HotSwapAgent"
            e.presentation.icon = VaadinIcons.RERUN_HOTSWAP
        } else {
            e.presentation.text = "Debug using HotSwapAgent"
            e.presentation.icon = VaadinIcons.DEBUG_HOTSWAP
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val executor = ExecutorRegistry.getInstance().getExecutorById(HotswapAgentExecutor.ID) ?: return
        val settings = RunManager.getInstance(project).selectedConfiguration ?: return
        ExecutionUtil.runConfiguration(settings, executor)
    }
}
