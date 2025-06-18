package com.vaadin.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.NewUiValue
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import com.vaadin.plugin.copilot.service.CopilotDotfileService
import com.vaadin.plugin.hotswapagent.JdkUtil
import com.vaadin.plugin.utils.doNotifyAboutVaadinProject
import com.vaadin.plugin.utils.hasEndpoints
import com.vaadin.plugin.utils.trackManualCopilotRestart
import java.awt.Color
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
        p.add(headerRow())
        p.add(copilotStatusRow())
        p.add(jbrStatusRow())
        p.add(endpointsStatusRow())
        add(p)
    }

    var refreshPopup: (() -> Unit)? = null

    private fun headerRow(): JComponent {
        val header = JBLabel("Vaadin plugin information")
        header.font = JBFont.h4()
        return header
    }

    private fun jbrStatusRow(): JComponent {
        val projectSdk = JdkUtil.getProjectSdk(project)
        if (projectSdk == null || !JdkUtil.isJetbrainsRuntime(projectSdk)) {
            return wrapMessage(
                "JetBrains Runtime unavailable",
                UIUtil.getLabelForeground(),
                createJbrDownloadButton(),
                "Download and setup latest JetBrains Runtime")
        }

        return wrapMessage("Jetbrains Runtime in use", UIUtil.getLabelSuccessForeground())
    }

    private fun copilotStatusRow(): JPanel {
        if (project.getService(CopilotDotfileService::class.java).isActive()) {
            return wrapMessage("Copilot service is running", UIUtil.getLabelSuccessForeground())
        }

        if (DumbService.isDumb(project)) {
            return wrapMessage(
                "Copilot service is not running",
                UIUtil.getLabelForeground(),
                "Service will be available after indexing is completed")
        }

        return wrapMessage(
            "Copilot service is not running",
            UIUtil.getLabelForeground(),
            createRestartButton(),
            "<html>Service will be started automatically.<br/>In case of issues you can restart it manually.</html>")
    }

    private fun endpointsStatusRow(): Component {
        if (hasEndpoints()) {
            return wrapMessage("Endpoints are available", UIUtil.getLabelSuccessForeground())
        }

        return wrapMessage(
            "Endpoints are not available",
            UIUtil.getLabelForeground(),
            "Feature is available for IntelliJ Ultimate with installed Endpoints plugin")
    }

    private fun createRestartButton(): JButton {
        val restart = JButton(AllIcons.Actions.Restart)
        restart.addActionListener {
            doNotifyAboutVaadinProject(project)
            DumbService.getInstance(project).smartInvokeLater {
                VaadinStatusBarWidget.update(project)
                refreshPopup?.invoke()
                trackManualCopilotRestart()
            }
        }
        return restart
    }

    private fun createJbrDownloadButton(): JButton {
        val downloadButton = JButton(AllIcons.Actions.Download)
        downloadButton.addActionListener {
            val action = ActionManager.getInstance().getAction("vaadin.jbr.install")
            val event =
                AnActionEvent.createFromAnAction(
                    action, null, ActionPlaces.UNKNOWN, DataManager.getInstance().getDataContext(this))
            action.actionPerformed(event)
            refreshPopup?.invoke()
        }
        return downloadButton
    }

    private fun createMessage(text: String, foreground: Color): JComponent {
        val message = JLabel()
        message.text = text
        message.foreground = foreground
        return message
    }

    private fun createDescription(text: String): JComponent {
        val desc = JLabel(text)
        desc.font = if (NewUiValue.isEnabled()) JBFont.medium() else JBFont.small()
        desc.foreground = UIUtil.getLabelInfoForeground()
        return desc
    }

    private fun wrapMessage(text: String, foreground: Color, description: String? = null): JPanel {
        val wrapper = JPanel(VerticalLayout())
        wrapper.add(createMessage(text, foreground))
        if (description != null) {
            wrapper.add(createDescription(description))
        }
        return wrapper
    }

    private fun wrapMessage(
        text: String,
        foreground: Color,
        component: Component,
        description: String? = null
    ): JPanel {
        val wrapper = JPanel(VerticalLayout())
        val horizontalPanel = JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP))
        horizontalPanel.add(createMessage(text, foreground))
        horizontalPanel.add(component)
        wrapper.add(horizontalPanel)
        if (description != null) {
            wrapper.add(createDescription(description))
        }
        return wrapper
    }
}
