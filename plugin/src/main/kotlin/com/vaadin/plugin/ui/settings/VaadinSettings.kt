package com.vaadin.plugin.ui.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.jetbrains.annotations.NotNull

@State(name = "com.vaadin.plugin.ui.settings.VaadinSettings", storages = [Storage("VaadinSettings.xml")])
internal class VaadinSettings : PersistentStateComponent<VaadinSettings.State> {
    internal class State {
        var userId: String? = null
        var sendUsageStatistics: Boolean = true
    }

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(@NotNull state: State) {
        myState = state
    }

    companion object {
        val instance: VaadinSettings
            get() = ApplicationManager.getApplication().getService(VaadinSettings::class.java)
    }
}
