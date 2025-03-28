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
        row { text("Include Walking Skeleton").bold() }
        row {
            text(
                "A walking skeleton is a minimal application that includes a fully-functional " +
                    "end-to-end workflow. All major building blocks are included, but it does not " +
                    "yet perform aby meaningful tasks.",
                MAX_LINE_LENGTH)
        }
        row("Pure Java with Vaadin Flow") { checkBox("").bindSelected(model.includeFlowProperty) }
        row("Full-stack React with Vaadin Hilla") { checkBox("").bindSelected(model.includeHillaProperty) }
    }
}
