package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.ProjectType
import com.vaadin.plugin.starter.DownloadableModel
import com.vaadin.plugin.utils.VaadinProjectUtil

class VaadinModuleBuilder : ModuleBuilder() {

    private val propertyGraph = PropertyGraph()

    private val projectDownloadedProperty = propertyGraph.property(false)

    private var model: DownloadableModel? = null

    override fun getBuilderId(): String {
        return "vaadin"
    }

    override fun getModuleType(): ModuleType<*> {
        return VaadinModuleType("VaadinModule")
    }

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?): ModuleWizardStep {
        return VaadinCustomOptionsStep(this)
    }

    fun setModel(model: DownloadableModel) {
        this.model = model
    }

    override fun createModule(moduleModel: ModifiableModuleModel): Module {
        val project = moduleModel.project
        project.putUserData(VaadinProjectUtil.PROJECT_DOWNLOADED_PROP_KEY, projectDownloadedProperty)
        VaadinProjectUtil.downloadAndExtract(project, this.model!!.getDownloadLink(project))
        return super.createModule(moduleModel)
    }

    override fun getProjectType(): ProjectType? {
        return this.model?.let { ProjectType.create(it.getProjectType()) }
    }

}
