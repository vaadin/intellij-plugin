package com.vaadin.plugin.copilot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.vaadin.plugin.copilot.handler.RedoHandler
import com.vaadin.plugin.copilot.handler.ShowInIdeHandler
import com.vaadin.plugin.copilot.handler.UndoHandler
import com.vaadin.plugin.copilot.handler.WriteFileHandler
import com.vaadin.plugin.copilot.service.CopilotServerService
import java.io.BufferedWriter
import java.io.File
import java.io.StringWriter
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path


class CopilotPluginUtil {

    companion object {

        private val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

        private const val DOTFILE = ".copilot-plugin"

        private const val VAADIN_LIB_PREFIX = "com.vaadin"

        private const val IDEA_DIR = ".idea"

        private const val NORMALIZED_LINE_SEPARATOR = "\n"

        private const val NOTIFICATION_GROUP = "Vaadin Copilot"

        private val COPILOT_ICON = IconLoader.getIcon("/icons/copilot.svg", CopilotPluginUtil::class.java)

        private enum class HANDLERS(val command: String) {
            WRITE("write"),
            UNDO("undo"),
            REDO("redo"),
            SHOW_IN_IDE("showInIde")
        }

        private val pluginVersion = PluginManagerCore.getPlugin(PluginId.getId("com.vaadin.intellij-plugin"))?.version

        fun isVaadinProject(project: Project): Boolean {
            if (project.basePath == null) {
                return false
            }

            val containsVaadinDeps = fun(file: String): Boolean {
                return Files.readString(Path(project.basePath!!, file)).contains(VAADIN_LIB_PREFIX)
            }

            // Maven projects
            if (File(project.basePath, "pom.xml").exists()) {
                return containsVaadinDeps("pom.xml")
            }

            // Gradle projects
            if (File(project.basePath, "build.gradle").exists()) {
                return containsVaadinDeps("build.gradle")
            }

            // Gradle Kotlin projects
            if (File(project.basePath, "build.gradle.kts").exists()) {
                return containsVaadinDeps("build.gradle.kts")
            }

            return false
        }

        fun notify(content: String, type: NotificationType) {
            notify(content, type, null)
        }

        fun notify(content: String, type: NotificationType, project: Project?) {
            Notifications.Bus.notify(
                Notification(NOTIFICATION_GROUP, content, type)
                    .setIcon(COPILOT_ICON), project
            )
        }

        fun isServerRunning(project: Project): Boolean {
            return project.service<CopilotServerService>().isRunning()
        }

        fun startServer(project: Project) {
            val server = project.service<CopilotServerService>()
            if (server.isRunning()) {
                LOG.info("Cannot start Vaadin Copilot integration as it is already started")
                return
            }
            server.init()
            savePortInDotFile(project, server.getPort()!!)
            ApplicationManager.getApplication().executeOnPooledThread {
                notify("Vaadin Copilot integration started", NotificationType.INFORMATION)
                server.start { data ->
                    handleClientData(project, data)
                }
            }
        }

        fun stopServer(project: Project) {
            val server = project.service<CopilotServerService>()
            if (!server.isRunning()) {
                LOG.info("Cannot stop Vaadin Copilot integration as it is not running")
                return
            }
            removeDotFile(project)
            server.stop()
            notify("Vaadin Copilot integration stopped", NotificationType.INFORMATION)
        }

        private fun handleClientData(project: Project, data: ByteArray) {
            val command: CommandRequest = jacksonObjectMapper().readValue(data)
            runInEdt {
                createCommandHandler(command.command, project, command.data)?.run()
            }
        }

        private fun createCommandHandler(
            command: String,
            project: Project,
            data: Map<String, Any>
        ): Runnable? {
            when (command) {
                HANDLERS.WRITE.command -> return WriteFileHandler(project, data)
                HANDLERS.UNDO.command -> return UndoHandler(project, data)
                HANDLERS.REDO.command -> return RedoHandler(project, data)
                HANDLERS.SHOW_IN_IDE.command -> return ShowInIdeHandler(project, data)
                else -> {
                    LOG.warn("Command $command not supported by plugin")
                }
            }
            return null
        }

        private fun savePortInDotFile(project: Project, port: Int) {
            val dotFileDirectory = getDotFileDirectory(project)
            if (dotFileDirectory != null) {
                val props = Properties()
                props.setProperty("port", port.toString())
                props.setProperty("ide", "intellij")
                props.setProperty("version", pluginVersion)
                props.setProperty("supportedActions", HANDLERS.values().map { a -> a.command }.joinToString(","))

                val stringWriter = StringWriter()
                val bufferedWriter = object : BufferedWriter(stringWriter) {
                    override fun newLine() {
                        write(NORMALIZED_LINE_SEPARATOR)
                    }
                }
                props.store(bufferedWriter, "Vaadin Copilot Integration Runtime Properties")

                val fileType = FileTypeManager.getInstance().getStdFileType("properties")
                runInEdt {
                    ApplicationManager.getApplication().runWriteAction {
                        dotFileDirectory.findFile(DOTFILE)?.delete()
                        val file = PsiFileFactory.getInstance(project)
                            .createFileFromText(DOTFILE, fileType, stringWriter.toString())
                        dotFileDirectory.add(file)
                        LOG.info("$DOTFILE created in ${dotFileDirectory.virtualFile.path}")
                    }
                }
            } else {
                LOG.error("Cannot create $DOTFILE")
            }
        }

        private fun removeDotFile(project: Project) {
            ApplicationManager.getApplication().runWriteAction {
                val dotFileDirectory = getDotFileDirectory(project)
                dotFileDirectory?.findFile(DOTFILE)?.let {
                    it.delete()
                    LOG.info("$DOTFILE removed from ${dotFileDirectory.virtualFile.path}")
                    return@runWriteAction
                }
                LOG.warn("Cannot remove $DOTFILE")
            }
        }

        private fun getIdeaDir(project: Project): File {
            return File(project.basePath, IDEA_DIR)
        }

        fun getDotFileDirectory(project: Project): PsiDirectory? {
            return ApplicationManager.getApplication().runReadAction<PsiDirectory?> {
                VfsUtil.findFileByIoFile(getIdeaDir(project), false)?.let {
                    return@runReadAction PsiManager.getInstance(project).findDirectory(it)
                }
                return@runReadAction null
            }
        }

        fun createIdeaDirectoryIfMissing(project: Project) {
            WriteCommandAction.runWriteCommandAction(project) {
                val ideaDir = getIdeaDir(project).path
                VfsUtil.createDirectoryIfMissing(ideaDir)?.let {
                    LOG.info("$ideaDir created")
                }
            }
        }
    }

}
