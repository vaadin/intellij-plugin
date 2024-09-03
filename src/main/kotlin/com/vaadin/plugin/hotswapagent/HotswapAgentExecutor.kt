package com.vaadin.plugin.hotswapagent

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.IconLoader.getIcon
import javax.swing.Icon


class HotswapAgentExecutor : DefaultDebugExecutor() {

    companion object {
        val ID = "Vaadin.HotswapAgentExecutor"
    }


    private val theIcon = IconLoader.getIcon("/icons/swap.svg", HotswapAgentExecutor::class.java.classLoader);

    override fun getDescription(): String {
        return "Debug using HotswapAgent"
    }

    override fun getId(): String {
        return Companion.ID;
    }

    override fun getToolWindowId(): String {
        return id;
    }

    override fun getActionName(): String {
        return description;
    }

    override fun getStartActionText(): String {
        return description;
    }

    override fun getStartActionText(configurationName: String): String {
        return description;
    }

    override fun getContextActionId(): String {
        return "$id-action";
    }

    override fun getIcon(): Icon {
        return theIcon;
    }

    override fun getToolWindowIcon(): Icon {
        return theIcon;
    }


    override fun getDisabledIcon(): Icon {
        return super.getDisabledIcon()
    }
}