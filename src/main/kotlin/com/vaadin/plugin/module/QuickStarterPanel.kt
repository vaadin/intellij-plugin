package com.vaadin.plugin.module

import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.vaadin.plugin.starter.QuickStarterModel

class QuickStarterPanel(private val model: QuickStarterModel) {

    val root = panel {
        row("Example views") {
            segmentedButton(QuickStarterModel.VIEWS) { this.text = it }.bind(model.exampleViewsProperty)
        }
        row("Use authentication") { checkBox("").bindSelected(model.useAuthenticationProperty) }
        row("Version") {
            segmentedButton(setOf(false, true)) { this.text = if (it) "Prerelease" else "Stable" }
                .bind(model.usePrereleaseProperty)
        }
    }
}
