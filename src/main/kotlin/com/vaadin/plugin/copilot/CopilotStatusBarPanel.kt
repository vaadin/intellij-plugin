package com.vaadin.plugin.copilot

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
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
        val group =
            ActionManager.getInstance().getAction("CopilotStatusBarActions") as? ActionGroup ?: return null

        return JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null, group, context,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false
            )
    }

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        val state = WidgetState("Vaadin", null, true)
        state.icon = IconLoader.getIcon("/icons/vaadin.svg", javaClass.classLoader)
        return state
    }
}
