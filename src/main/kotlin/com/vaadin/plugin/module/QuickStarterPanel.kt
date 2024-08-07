package com.vaadin.plugin.module

import com.intellij.ui.dsl.builder.panel
import com.vaadin.plugin.starter.QuickStarterModel

class QuickStarterPanel {

    val model = QuickStarterModel("Flow (Java)", false, "Stable")

    val root = panel {
        row("Example views") {
            segmentedButton(setOf("Flow (Java)", "Hilla (React)", "None")) { this.text = it }.whenItemSelected {
                model.views = it
            }.selectedItem = model.views
        }
        row("Use authentication") {
            checkBox("").onChanged {
                model.authentication = it.isSelected
            }.component.isSelected = model.authentication
        }
        row("Version") {
            segmentedButton(setOf("Stable", "Prerelease")) { this.text = it }.whenItemSelected {
                model.version = it
            }.selectedItem = model.version
        }
    }

}