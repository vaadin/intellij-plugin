package com.vaadin.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.NewUiValue
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.utils.hasEndpoints
import com.vaadin.plugin.utils.trackManualCopilotRestart
import java.awt.Component
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import org.jdesktop.swingx.HorizontalLayout
import org.jdesktop.swingx.VerticalLayout

class VaadinStatusBarInfoPopupPanel(private val project: Project) : JPanel() {

    init {
        val p = DialogPanel(VerticalLayout(UIUtil.LARGE_VGAP))
        p.border = JBEmptyBorder(UIUtil.getRegularPanelInsets())

        val header = JBLabel("Vaadin plugin information")
        header.font = JBFont.h4()

        p.add(header)
        p.add(copilotStatusRow())
        p.add(endpointsStatusRow())
        add(p)
    }

    var afterRestart: (() -> Unit)? = null

    private fun copilotStatusRow(): JPanel {
        val status = CopilotPluginUtil.isActive(project)
        val wrapper = JPanel(VerticalLayout())

        val panel = JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP))
        val message = JLabel()
        if (status) {
            message.text = "Copilot service is running"
            message.foreground = UIUtil.getLabelSuccessForeground()
        } else {
            message.text = "Copilot service is not running"
            message.foreground = UIUtil.getLabelForeground()
        }
        panel.add(message)
        wrapper.add(panel)

        if (!status) {
            if (DumbService.isDumb(project)) {
                wrapper.add(createDescription("Service will be available after indexing is completed"))
            } else {
                val restart = JButton(AllIcons.Actions.Restart)
                restart.addActionListener {
                    CopilotPluginUtil.removeDotFile(project)
                    CopilotPluginUtil.saveDotFile(project)
                    trackManualCopilotRestart()
                    DumbService.getInstance(project).smartInvokeLater {
                        VaadinStatusBarWidget.update(project)
                        afterRestart?.invoke()
                    }
                }
                panel.add(restart)
                wrapper.add(
                    createDescription(
                        "<html>Service will be started automatically.<br/>In case of issues you can restart it manually.</html>"))
            }
        }
        return wrapper
    }

    private fun endpointsStatusRow(): Component {
        val wrapper = JPanel(VerticalLayout())
        val message = JLabel()
        wrapper.add(message)
        if (hasEndpoints()) {
            message.text = "Endpoints are available"
            message.foreground = UIUtil.getLabelSuccessForeground()
        } else {
            message.text = "Endpoints are not available"
            message.foreground = UIUtil.getLabelForeground()
            wrapper.add(createDescription("Feature is available for IntelliJ Ultimate with installed Endpoints plugin"))
        }
        return wrapper
    }

    private fun createDescription(text: String): JComponent {
        val desc = JLabel(text)
        desc.font = if (NewUiValue.isEnabled()) JBFont.medium() else JBFont.small()
        desc.foreground = UIUtil.getLabelInfoForeground()

        return desc
    }
}
