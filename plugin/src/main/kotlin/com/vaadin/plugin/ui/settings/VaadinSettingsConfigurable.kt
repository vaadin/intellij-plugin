package com.vaadin.plugin.ui.settings

import com.intellij.openapi.options.Configurable
import java.util.Objects
import javax.swing.JComponent

/** Provides controller functionality for application settings. */
internal class VaadinSettingsConfigurable : Configurable {

    private var mySettingsComponent: VaadinSettingsComponent? = null

    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        mySettingsComponent = VaadinSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val state: VaadinSettings.State = Objects.requireNonNull(VaadinSettings.instance.state)
        return mySettingsComponent!!.sendUsageStatisticsStatus != state.sendUsageStatistics
    }

    override fun apply() {
        val state: VaadinSettings.State = Objects.requireNonNull(VaadinSettings.instance.state)
        state.sendUsageStatistics = mySettingsComponent!!.sendUsageStatisticsStatus
    }

    override fun reset() {
        val state: VaadinSettings.State = Objects.requireNonNull(VaadinSettings.instance.state)
        mySettingsComponent!!.sendUsageStatisticsStatus = state.sendUsageStatistics
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }

    override fun getDisplayName(): String {
        return "Vaadin"
    }
}
