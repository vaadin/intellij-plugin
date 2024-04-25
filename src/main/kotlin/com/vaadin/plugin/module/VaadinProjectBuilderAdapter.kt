package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.vaadin.plugin.utils.VaadinProjectUtil
import java.io.File

class VaadinProjectBuilderAdapter(private val vaadinWizard: VaadinProjectWizard = VaadinProjectWizard()) :
    GeneratorNewProjectWizardBuilderAdapter(vaadinWizard) {

    private val propertyGraph = PropertyGraph()

    private val projectDownloadedProperty = propertyGraph.property(false)

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        return vaadinWizard.createStep(context)
    }

    override fun createProject(name: String?, path: String?): Project? {
        return super.createProject(name, path)?.let { project ->
            project.putUserData(VaadinProjectUtil.PROJECT_DOWNLOADED_PROP_KEY, projectDownloadedProperty)
            projectDownloadedProperty.afterChange { afterProjectCreated(project) }
            VaadinProjectUtil.downloadAndExtract(project, vaadinWizard.projectModel!!.getDownloadLink(project))
            project
        }
    }

    override fun canCreateModule(): Boolean {
        return false
    }

    private fun afterProjectCreated(project: Project) {
        VaadinProjectUtil.notify("Vaadin project created", NotificationType.INFORMATION, project)
        VfsUtil.findFileByIoFile(File(project.basePath, "README.md"), true)?.let {
            val descriptor = OpenFileDescriptor(project, it)
            descriptor.setUsePreviewTab(true)
            FileEditorManager.getInstance(project).openEditor(descriptor, true)
        }
    }

}