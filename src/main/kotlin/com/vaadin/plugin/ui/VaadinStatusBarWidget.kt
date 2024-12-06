package com.vaadin.plugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBarWidget
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
    }

    private val iconSupplier: BadgeIconSupplier = BadgeIconSupplier(VaadinIcons.VAADIN)

    override fun ID(): String {
        return ID
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return this
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer {
            val popup = VaadinStatusBarInfoPopupPanel(isCopilotActive(), hasEndpoints())
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popup, null)
                .createPopup()
                .show(RelativePoint.fromScreen(it.locationOnScreen))
        }
    }

    override fun getTooltipText(): String {
        if (!isCopilotActive() || !hasEndpoints()) {
            return "There are issues while running Vaadin plugin, click to see details"
        }

        return "Vaadin plugin is active"
    }

    override fun getIcon(): Icon {
        if (!isCopilotActive() || !hasEndpoints()) {
            return iconSupplier.warningIcon
        }

        return iconSupplier.originalIcon
    }

    private fun isCopilotActive(): Boolean {
        return CopilotPluginUtil.getDotFile(project) !== null
    }
}
