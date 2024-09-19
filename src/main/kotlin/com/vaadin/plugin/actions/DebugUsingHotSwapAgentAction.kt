package com.vaadin.plugin.actions

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.dashboard.actions.ExecutorAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.vaadin.plugin.hotswapagent.HotswapAgentExecutor

class DebugUsingHotSwapAgentAction : ExecutorAction() {
    override fun update(p0: AnActionEvent, p1: Boolean) {}

    override fun getExecutor(): Executor {
        return ExecutorRegistry.getInstance().getExecutorById(HotswapAgentExecutor.ID)!!
    }
}
