package com.vaadin.plugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.BadgeIconSupplier
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.utils.VaadinIcons
import com.vaadin.plugin.utils.hasEndpoints
import java.awt.event.MouseEvent
import javax.swing.Icon

class VaadinStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.IconPresentation {

    companion object {
        const val ID = "VaadinStatusBarPanel"

        fun update(project: Project) {
            WindowManager.getInstance().getStatusBar(project).updateWidget(ID)
        }
    }

    private val iconSupplier: BadgeIconSupplier = BadgeIconSupplier(VaadinIcons.VAADIN)

    override fun ID(): String {
        return ID
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return this
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer { showPopup(RelativePoint.fromScreen(it.locationOnScreen)) }
    }

    private fun showPopup(relativePoint: RelativePoint) {
        val panel = VaadinStatusBarInfoPopupPanel(project)
        val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null).createPopup()
        panel.afterRestart = {
            popup.cancel()
            showPopup(relativePoint)
        }
        popup.show(relativePoint)
    }

    override fun getTooltipText(): String {
        if (!CopilotPluginUtil.isActive(project) || !hasEndpoints()) {
            return "There are issues while running Vaadin plugin, click to see details"
        }

        return "Vaadin plugin is active"
    }

    override fun getIcon(): Icon {
        if (!CopilotPluginUtil.isActive(project) || !hasEndpoints()) {
            return iconSupplier.warningIcon
        }

        return iconSupplier.originalIcon
    }
}
