package com.vaadin.plugin.ui

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.ui.BadgeIconSupplier
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.utils.VaadinIcons
import com.vaadin.plugin.utils.hasEndpoints
import javax.swing.Icon

class VaadinStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {

    companion object {
        const val ID = "VaadinStatusBarPanel"
    }

    private val iconSupplier: BadgeIconSupplier = BadgeIconSupplier(VaadinIcons.VAADIN)

    private var clicked: Boolean = false

    override fun ID(): String {
        return ID
    }

    override fun createInstance(project: Project): StatusBarWidget {
        return VaadinStatusBarWidget(project)
    }

    override fun createPopup(context: DataContext): ListPopup? {
        clicked = true
        val popup = VaadinStatusBarInfoPopupPanel(isCopilotActive(), hasEndpoints())
        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(popup, null)
            .createPopup()
            .showInBestPositionFor(context)
        return null
    }

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        val state = WidgetState(getTooltip(), null, true)
        state.icon = getIcon()
        return state
    }

    private fun getTooltip(): String {
        if (!isCopilotActive() || !hasEndpoints()) {
            return "There are issues while running Vaadin plugin, click to see details"
        }

        return "Vaadin plugin is active"
    }

    private fun getIcon(): Icon {
        if (clicked) {
            return iconSupplier.originalIcon
        }

        if (!isCopilotActive() || !hasEndpoints()) {
            return iconSupplier.warningIcon
        }

        return iconSupplier.originalIcon
    }

    private fun isCopilotActive(): Boolean {
        return CopilotPluginUtil.getDotFile(project) !== null
    }
}
