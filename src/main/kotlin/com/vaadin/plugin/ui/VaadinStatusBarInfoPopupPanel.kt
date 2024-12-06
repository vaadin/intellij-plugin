package com.vaadin.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JPanel
import org.jdesktop.swingx.HorizontalLayout
import org.jdesktop.swingx.VerticalLayout

class VaadinStatusBarInfoPopupPanel(copilotInitialized: Boolean, endpointsAvailable: Boolean) : JPanel() {

    init {
        val p = DialogPanel(VerticalLayout(UIUtil.LARGE_VGAP))
        p.border = JBEmptyBorder(UIUtil.getRegularPanelInsets())
        add(p)

        val header = JBLabel("Vaadin plugin information")
        header.font = JBFont.h4()

        p.add(header)
        p.add(
            statusRow(
                "Copilot service is running",
                copilotInitialized,
                "Service will be available after indexing is completed"))
        p.add(
            statusRow(
                "Endpoints are available",
                endpointsAvailable,
                "Feature is available for IntelliJ Ultimate with installed Endpoints plugin"))
    }

    private fun statusRow(label: String, checked: Boolean, description: String): Component {
        val wrapper = JPanel(VerticalLayout())

        val panel = JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP))
        panel.add(JBLabel(label))
        panel.add(JLabel(if (checked) AllIcons.Actions.Checked else AllIcons.Actions.Cancel))
        wrapper.add(panel)

        if (!checked) {
            val desc = JLabel(description)
            desc.font = JBFont.smallOrNewUiMedium().asItalic()
            wrapper.add(desc)
        }
        return wrapper
    }
}
