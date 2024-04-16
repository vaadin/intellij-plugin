package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import javax.swing.JComponent

class VaadinCustomOptionsStep(private val builder: VaadinModuleBuilder) : ModuleWizardStep() {

    private val panel = VaadinPanel()

    override fun getComponent(): JComponent {
        return panel
    }

    override fun updateDataModel() {
        builder.setModel(panel.getModel())
    }

}
