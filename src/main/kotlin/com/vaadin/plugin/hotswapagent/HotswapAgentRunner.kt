package com.vaadin.plugin.hotswapagent

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.RunProfile

class HotswapAgentRunner: GenericDebuggerRunner() {

    override fun getRunnerId(): String {
        return "vaadin-hotswapagent-runner";
    }
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId.equals(HotswapAgentExecutor.ID);
    }
}