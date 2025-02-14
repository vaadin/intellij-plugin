package com.vaadin.plugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class VaadinStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String {
        return "VaadinStatusBarWidgetFactory"
    }

    override fun getDisplayName(): String {
        return "Vaadin"
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return VaadinStatusBarWidget(project)
    }
}
