package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.dsl.builder.Panel

class VaadinProjectWizardStep(override val context: WizardContext, override val propertyGraph: PropertyGraph) :
    NewProjectWizardStep {

    override val data: UserDataHolder
        get() = UserDataHolderBase()

    override val keywords: NewProjectWizardStep.Keywords
        get() = NewProjectWizardStep.Keywords()

    override fun setupUI(builder: Panel) {
        VaadinPanel(propertyGraph, context, builder)
    }
}
