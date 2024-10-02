package com.vaadin.plugin

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtil
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.utils.VaadinProjectUtil
import com.vaadin.plugin.utils.VaadinProjectUtil.Companion.findVaadinModule
import java.io.File
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.gradle.settings.GradleSettings

class VaadinProjectDetector : ModuleRootListener, ProjectActivity {

    private val LOG: Logger = Logger.getInstance(VaadinProjectDetector::class.java)

    override fun rootsChanged(event: ModuleRootEvent) {
        if (event.project.isOpen) {
            detectVaadinAndNotify(event.project)
        }
    }

    override suspend fun execute(project: Project) {
        detectVaadinAndNotify(project)
    }

    private fun detectVaadinAndNotify(project: Project) {
        findVaadinModule(project)?.let { module ->
            LOG.info("Detected Vaadin module: ${module.name}")
            val projectRoot = findProjectRoot(project, module)
            if (projectRoot == null) {
                LOG.warn("Project root not found")
                return@let
            }
            val projectRootFile = VfsUtil.findFileByIoFile(File(projectRoot), true)
            if (projectRootFile == null) {
                LOG.warn("Project root $projectRootFile is not accessible")
                return@let
            }
            project.putUserData(VaadinProjectUtil.VAADIN_ROOT_MODULE_PATH_KEY, projectRootFile)
            project.messageBus.syncPublisher(VaadinProjectListener.TOPIC).vaadinProjectDetected(project)
        }
    }

    private fun findProjectRoot(project: Project, module: Module): String? {
        val maybeMavenProject = MavenProjectsManager.getInstance(project)
        if (maybeMavenProject.isMavenizedProject) {
            val rootDir = maybeMavenProject.rootProjects.iterator().next().directory
            LOG.info("Maven project root: $rootDir")
            return rootDir
        }

        val maybeGradle = GradleSettings.getInstance(project)
        if (!maybeGradle.linkedProjectsSettings.isEmpty()) {
            val externalProjectPath = maybeGradle.linkedProjectsSettings.iterator().next().externalProjectPath
            if (externalProjectPath != null) {
                LOG.info("Gradle project root: $externalProjectPath")
                return externalProjectPath
            }
        }

        val externalRootPath = ExternalSystemApiUtil.getExternalRootProjectPath(module)
        if (externalRootPath != null) {
            LOG.info("External system root: $externalRootPath")
            return externalRootPath
        }

        LOG.info("Project base path: ${project.basePath}")
        return project.basePath
    }
}
