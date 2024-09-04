package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.PropertyGraph
import com.vaadin.plugin.starter.DownloadableModel
import com.vaadin.plugin.utils.VaadinIcons
import com.vaadin.plugin.utils.VaadinProjectUtil.Companion.PROJECT_MODEL_PROP_KEY
import javax.swing.Icon

class VaadinProjectWizard : GeneratorNewProjectWizard {

    override val icon: Icon
        get() = VaadinIcons.VAADIN_BLUE

    override val id: String
        get() = "Vaadin"

    override val name: String
        get() = "Vaadin"

    private val propertyGraph: PropertyGraph
        get() = PropertyGraph("Vaadin project")

    private val projectModelProperty = propertyGraph.property<DownloadableModel?>(null)

    val projectModel: DownloadableModel? by projectModelProperty

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        context.putUserData(PROJECT_MODEL_PROP_KEY, projectModelProperty)
        return VaadinProjectWizardStep(context, propertyGraph)
    }

}
