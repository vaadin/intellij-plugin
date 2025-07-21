package com.vaadin.plugin.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.JBCefBrowser

class VaadinChatAppToolWindowFactory : ToolWindowFactory, DumbAware {

    val title = "Vaadin AI Chat App"

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        toolWindow.title = title
        toolWindow.stripeTitle = title

        val browser = JBCefBrowser("http://localhost:9090")
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(browser.component, null, true)
        contentManager.addContent(content)
    }
}
