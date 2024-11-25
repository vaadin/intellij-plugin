package com.vaadin.plugin.hotswapagent

import com.intellij.execution.executors.DefaultDebugExecutor
import com.vaadin.plugin.utils.VaadinIcons
import javax.swing.Icon

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
}
