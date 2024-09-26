package com.vaadin.plugin.actions

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.dashboard.actions.ExecutorAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.vaadin.plugin.hotswapagent.HotswapAgentExecutor
import com.vaadin.plugin.utils.VaadinIcons

class DebugUsingHotSwapAgentAction : ExecutorAction() {
    override fun update(event: AnActionEvent, isRunning: Boolean) {
        if (isRunning) {
            event.presentation.text = "Rerun using HotSwapAgent"
            // TODO: https://github.com/vaadin/intellij-plugin/issues/101
            event.presentation.icon = VaadinIcons.DEBUG_HOTSWAP
        } else {
            event.presentation.text = "Debug using HotSwapAgent"
            event.presentation.icon = VaadinIcons.DEBUG_HOTSWAP
        }
    }

    override fun getExecutor(): Executor {
        return ExecutorRegistry.getInstance().getExecutorById(HotswapAgentExecutor.ID)!!
    }
}
