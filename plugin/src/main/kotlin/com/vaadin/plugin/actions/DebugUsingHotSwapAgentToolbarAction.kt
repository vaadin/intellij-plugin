package com.vaadin.plugin.actions

import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.vaadin.plugin.hotswapagent.HotswapAgentExecutor
import com.vaadin.plugin.utils.VaadinIcons

class DebugUsingHotSwapAgentToolbarAction : AnAction(), DumbAware {

    // EDT because update() reads run-content UI state (RunContentManager.getAllDescriptors).
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val executor = ExecutorRegistry.getInstance().getExecutorById(HotswapAgentExecutor.ID)
        if (project == null || executor == null || !executor.isApplicable(project)) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        e.presentation.isEnabledAndVisible = true
        // The selected configuration is running when it has a live run content. Uses the public,
        // side-effect-free RunContentManager.getAllDescriptors() rather than the internal
        // ExecutionManager.getRunningDescriptors(). findContentDescriptor() is avoided because it
        // lazily creates/decorates the executor tool window, which must happen on the EDT.
        val selected = RunManager.getInstance(project).selectedConfiguration
        val running =
            selected != null &&
                RunContentManager.getInstance(project).allDescriptors.any { descriptor ->
                    descriptor.processHandler?.isProcessTerminated == false && descriptor.displayName == selected.name
                }
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
