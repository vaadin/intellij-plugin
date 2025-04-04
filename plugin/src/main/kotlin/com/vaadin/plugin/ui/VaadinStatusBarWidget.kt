package com.vaadin.plugin.ui

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.IconManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.intellij.util.ui.JBUI.CurrentTheme.IconBadge
import com.intellij.util.ui.UIUtil
import com.vaadin.plugin.copilot.service.CopilotDotfileService
import com.vaadin.plugin.utils.VaadinIcons
import com.vaadin.plugin.utils.hasEndpoints
import com.vaadin.plugin.utils.trackManualCopilotRestart
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon

class VaadinStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.IconPresentation {

    companion object {
        const val ID = "VaadinStatusBarPanel"

        fun update(project: Project) {
            WindowManager.getInstance().getStatusBar(project)?.updateWidget(ID)
        }
    }

    private var clicked: Boolean = false

    init {
        project.messageBus
            .connect()
            .subscribe(
                DumbService.DUMB_MODE,
                object : DumbService.DumbModeListener {
                    override fun enteredDumbMode() {
                        update(project)
                    }

                    override fun exitDumbMode() {
                        update(project)
                    }
                })
    }

    override fun ID(): String {
        return ID
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return this
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer {
            clicked = true
            showPopup(it)
            update(project)
        }
    }

    private fun showPopup(e: MouseEvent) {
        val panel = VaadinStatusBarInfoPopupPanel(project)
        val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null).createPopup()
        panel.afterRestart = {
            popup.cancel()
            showPopup(e)
            trackManualCopilotRestart()
        }
        val dimension = popup.content.preferredSize
        val at = Point(0, -dimension.height)
        popup.show(RelativePoint(e.component, at))
        // destroy popup on unexpected project close
        Disposer.register(this, popup)
    }

    override fun getTooltipText(): String {
        if (isActive() && hasEndpoints()) {
            return "Vaadin plugin is active, all features are available"
        }

        if (DumbService.isDumb(project)) {
            return "Indexing is in progress, please wait"
        }

        return "Not all features are available, click to see details"
    }

    override fun getIcon(): Icon {
        if (clicked) {
            return VaadinIcons.VAADIN
        }

        if (isActive() && hasEndpoints()) {
            return VaadinIcons.VAADIN
        }

        if (DumbService.isDumb(project)) {
            return IconManager.getInstance().withIconBadge(VaadinIcons.VAADIN, UIUtil.getLabelForeground())
        }

        return IconManager.getInstance().withIconBadge(VaadinIcons.VAADIN, IconBadge.WARNING)
    }

    private fun isActive(): Boolean {
        return project.getService(CopilotDotfileService::class.java).isActive()
    }
}
