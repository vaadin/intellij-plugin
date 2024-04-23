package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.ProjectType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.VfsUtil
import com.vaadin.plugin.starter.DownloadableModel
import com.vaadin.plugin.utils.VaadinProjectUtil
import java.io.File

class VaadinModuleBuilder : ModuleBuilder() {

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

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val project = modifiableRootModel.project
        StartupManager.getInstance(project).runAfterOpened {
            VaadinProjectUtil.downloadAndExtract(project, this.model!!.getDownloadLink(project)) {
                VaadinProjectUtil.notify("Vaadin project created", NotificationType.INFORMATION, project)
                VfsUtil.findFileByIoFile(File(project.basePath, "README.md"), true)?.let {
                    val descriptor = OpenFileDescriptor(project, it)
                    descriptor.setUsePreviewTab(true)
                    FileEditorManagerEx.getInstanceEx(project).openEditor(descriptor, true)
                }
            }
        }
    }

    override fun getProjectType(): ProjectType? {
        return this.model?.let { ProjectType.create(it.getProjectType()) }
    }

}
