package com.vaadin.plugin.copilot

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup

class CopilotStatusBarPanel(project: Project) : EditorBasedStatusBarPopup(project, false) {

    override fun ID(): String {
        return "CopilotStatusBarPanel"
    }

    override fun createInstance(project: Project): StatusBarWidget {
        return CopilotStatusBarPanel(project)
    }

    override fun createPopup(context: DataContext): ListPopup? {
        return null
    }

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        val state = WidgetState("Vaadin plugin is active", null, true)
        state.icon = IconLoader.getIcon("/icons/vaadin.svg", javaClass.classLoader)
        return state
    }
}
