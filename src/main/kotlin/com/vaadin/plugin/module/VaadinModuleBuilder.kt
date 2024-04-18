package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.vaadin.plugin.starter.HasDownloadLink
import com.vaadin.plugin.utils.VaadinProjectUtil

class VaadinModuleBuilder : ModuleBuilder() {

    private var model: HasDownloadLink? = null

    override fun getBuilderId(): String {
        return "vaadin"
    }

    override fun getModuleType(): ModuleType<*> {
        return VaadinModuleType("VaadinModule")
    }

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep {
        return VaadinCustomOptionsStep(this)
    }

    fun setModel(model: HasDownloadLink) {
        this.model = model
    }

    override fun createModule(moduleModel: ModifiableModuleModel): Module {
        VaadinProjectUtil.downloadAndExtract(moduleModel.project, this.model!!.getDownloadLink(moduleModel.project))
        return super.createModule(moduleModel)
    }

}
