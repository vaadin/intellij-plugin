package com.vaadin.plugin.module

import com.intellij.ide.actions.NewProjectAction
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.io.ZipUtil
import com.vaadin.plugin.utils.DownloadUtil
import com.vaadin.plugin.utils.IdeUtil
import com.vaadin.plugin.utils.IdeUtil.getIdeaDirectoryPath
import com.vaadin.plugin.utils.getVaadinPluginDescriptor
import com.vaadin.plugin.utils.trackProjectCreated
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class VaadinProjectBuilderAdapter(private val vaadinWizard: VaadinProjectWizard = VaadinProjectWizard()) :
    GeneratorNewProjectWizardBuilderAdapter(vaadinWizard) {

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        return vaadinWizard.createStep(context)
    }

    override fun createProject(name: String?, path: String?): Project? {
        return super.createProject(name, path)?.let { project ->
            val outputPath = Path.of(path!!)
            val downloadUrl = vaadinWizard.projectModel!!.getDownloadLink(project)
            val tempFile = FileUtil.generateRandomTemporaryPath("project", ".zip")
            DownloadUtil.download(project, downloadUrl, tempFile.toPath(), "Vaadin Project")
                .thenRun {
                    // project zip contains single root directory, move contents to parent after
                    // extracting
                    val zipRoot = DownloadUtil.getZipRootFolder(tempFile)!!
                    ZipUtil.extract(tempFile.toPath(), outputPath, null, true)
                    IdeUtil.moveDirectoryContentsRobustly(outputPath.resolve(zipRoot).toFile(), outputPath.toFile())
                    FileUtil.delete(tempFile)
                    copyVaadinIcon(project)
                }
                .thenRun {
                    afterProjectCreated(project)
                    trackProjectCreated(downloadUrl)
                }
            project
        }
    }

    // Show wizard only for new projects, not modules
    override fun isAvailable(): Boolean {
        return Thread.currentThread().stackTrace.any { element ->
            element.className.contains(NewProjectAction::class.java.name)
        }
    }

    private fun afterProjectCreated(project: Project) {
        VfsUtil.findFileByIoFile(File(project.basePath, "README.md"), true)?.let {
            val descriptor = OpenFileDescriptor(project, it)
            descriptor.isUsePreviewTab = true
            FileEditorManager.getInstance(project).openEditor(descriptor, true)
        }
    }

    private fun copyVaadinIcon(project: Project) {
        val ideaDir = getIdeaDirectoryPath(project)
        if (ideaDir != null) {
            FileUtil.createIfDoesntExist(ideaDir.toFile())
            val classLoader = getVaadinPluginDescriptor().classLoader
            classLoader.getResourceAsStream("icon.png")?.use { inputStream ->
                Files.copy(inputStream, ideaDir.resolve("icon.png"))
            }
        }
    }
}
