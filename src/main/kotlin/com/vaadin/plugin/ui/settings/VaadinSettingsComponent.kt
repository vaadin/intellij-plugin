package com.vaadin.plugin.ui.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/** Supports creating and managing a [JPanel] for the Settings Dialog. */
class VaadinSettingsComponent {
    val panel: JPanel
    private val sendUsageStatistics = JBCheckBox("Send usage statistics")

    init {
        panel =
            FormBuilder.createFormBuilder()
                .addComponent(sendUsageStatistics, 1)
                .addTooltip("Help us improve Vaadin plugin by sending anonymous usage statistics")
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
