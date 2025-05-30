package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.vaadin.plugin.utils.VaadinProjectUtil
import com.vaadin.plugin.utils.trackProjectCreated
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
            val downloadLink = vaadinWizard.projectModel!!.getDownloadLink(project)
            VaadinProjectUtil.downloadAndExtractProject(project, downloadLink)
            trackProjectCreated(downloadLink)
            project
        }
    }

    override fun isAvailable(): Boolean {
        val lastPerformedActionId = (ActionManager.getInstance() as ActionManagerImpl).lastPreformedActionId
        lastPerformedActionId ?: return false
        return lastPerformedActionId.contains("NewProject", true)
    }

    private fun afterProjectCreated(project: Project) {
        VfsUtil.findFileByIoFile(File(project.basePath, "README.md"), true)?.let {
            val descriptor = OpenFileDescriptor(project, it)
            descriptor.setUsePreviewTab(true)
            FileEditorManager.getInstance(project).openEditor(descriptor, true)
        }
    }
}
