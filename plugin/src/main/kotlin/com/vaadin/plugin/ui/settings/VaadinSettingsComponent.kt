package com.vaadin.plugin.ui.settings

import com.intellij.ide.BrowserUtil
import com.intellij.ui.ColorUtil
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.scale.JBUIScale.scale
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper
import com.intellij.util.ui.UIUtil
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent
import javax.swing.text.html.HTMLDocument

/** Supports creating and managing a [JPanel] for the Settings Dialog. */
class VaadinSettingsComponent {
    val panel: JPanel
    private val sendUsageStatistics = JBCheckBox("Send usage statistics")

    init {
        val pane = JPanel(GridBagLayout())
        val viewer = SwingHelper.createHtmlViewer(true, null, JBColor.WHITE, JBColor.BLACK)
        viewer.isOpaque = false
        viewer.isFocusable = false
        UIUtil.doNotScrollToCaret(viewer)
        viewer.addHyperlinkListener(
            object : HyperlinkAdapter() {
                override fun hyperlinkActivated(e: HyperlinkEvent) {
                    e.url?.let { BrowserUtil.browse(it) }
                }
            })
        viewer.text =
            "<html><body>" +
                "Help us improve Vaadin plugin by sending anonymous usage statistics. <br/><br/>" +
                "Please note that this will not include personal data or any sensitive information, <br/>" +
                "such as source code, file names, etc. The data sent complies with the <a href=\"https://vaadin.com/privacy-policy\">Vaadin Privacy Policy</a>." +
                "</html></body>"
        val styleSheet = (viewer.document as HTMLDocument).styleSheet
        styleSheet.addRule(
            "body {font-size: " +
                UIUtil.getFontSize(UIUtil.FontSize.SMALL) +
                "; color: #" +
                ColorUtil.toHex(UIUtil.getContextHelpForeground()) +
                ";}")
        pane.add(
            viewer,
            GridBagConstraints(
                0,
                GridBagConstraints.RELATIVE,
                1,
                1,
                1.0,
                0.0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH,
                JBUI.insets(0, scale(24), 0, 0),
                0,
                0))

        panel =
            FormBuilder.createFormBuilder()
                .addComponent(sendUsageStatistics, 1)
                .addComponent(pane)
                .addComponentFillVertically(JPanel(), 0)
                .panel
    }

    val preferredFocusedComponent: JComponent
        get() = sendUsageStatistics

    var sendUsageStatisticsStatus: Boolean
        get() = sendUsageStatistics.isSelected
        set(newStatus) {
            sendUsageStatistics.isSelected = newStatus
        }
}
