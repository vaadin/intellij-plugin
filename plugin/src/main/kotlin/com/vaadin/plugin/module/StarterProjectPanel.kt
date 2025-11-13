package com.vaadin.plugin.module

import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.vaadin.plugin.starter.StarterProjectModel

class StarterProjectPanel(private val model: StarterProjectModel) {

    private val MAX_LINE_LENGTH = 60

    val root = panel {
        row("Vaadin Version") {
            segmentedButton(setOf(false, true)) { this.text = if (it) "Prerelease" else "Stable" }
                .bind(model.usePrereleaseProperty)
        }
        row { text("Include sample view").bold() }
        row { text("A sample view built fully in Java, front to back.", MAX_LINE_LENGTH) }
        row("Include sample view") { checkBox("").bindSelected(model.includeFlowProperty) }
    }
}
