package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.ProjectType
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.vaadin.plugin.starter.StarterModel
import com.vaadin.plugin.utils.ProjectUtil


class VaadinModuleBuilder : ModuleBuilder() {

    private var model: StarterModel? = null

    override fun getBuilderId(): String {
        return "vaadin"
    }

    override fun getModuleType(): ModuleType<*> {
        return VaadinModuleType("VaadinModule")
    }

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        return emptyArray()
    }

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep {
        return VaadinCustomOptionsStep(this)
    }

    fun setModel(model: StarterModel) {
        this.model = model
    }

    override fun createModule(moduleModel: ModifiableModuleModel): Module {
        ProjectUtil.downloadAndExtract(moduleModel.project, this.model!!.downloadLink())
        return super.createModule(moduleModel)
    }

    override fun getProjectType(): ProjectType? {
        return ProjectType.create(model?.language)
    }

}
