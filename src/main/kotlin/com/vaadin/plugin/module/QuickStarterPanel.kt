package com.vaadin.plugin.module

import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.vaadin.plugin.starter.QuickStarterModel

class QuickStarterPanel {

    val model = QuickStarterModel("Flow", "React", true, false, "Stable")

    private var frontendRow: Row? = null

    val root = panel {
        row("Framework") {
            segmentedButton(setOf("Flow", "Hilla")) { it }.whenItemSelected {
                model.framework = it
                updateFrontendRowVisible()
            }.selectedItem = model.framework
        }
        frontendRow = row("Frontend") {
            segmentedButton(setOf("React", "Lit")) { it }.whenItemSelected {
                model.frontend = it
            }.selectedItem = model.frontend
        }
        row("Add example views?") {
            checkBox("").bindSelected(model::exampleViews)
        }
        row("Add authentication?") {
            checkBox("").bindSelected(model::authentication)
        }
        row("Version") {
            segmentedButton(setOf("Stable", "Prerelease")) { it }.whenItemSelected {
                model.version = it
            }.selectedItem = model.version
        }
        updateFrontendRowVisible()
    }

    private fun updateFrontendRowVisible() {
        frontendRow?.visible(model.framework == "Hilla")
    }

}