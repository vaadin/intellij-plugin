package com.vaadin.plugin.copilot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.vaadin.plugin.copilot.handler.RedoHandler
import com.vaadin.plugin.copilot.handler.UndoHandler
import com.vaadin.plugin.copilot.handler.WriteFileHandler
import com.vaadin.plugin.copilot.service.CopilotServerService
import java.io.File
import java.io.StringWriter
import java.util.Properties


class CopilotPluginUtil {

    companion object {

        private val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

        private const val DOTFILE = ".copilot-plugin"

        private enum class HANDLERS(val command: String) {
            WRITE("write"),
            UNDO("undo"),
            REDO("redo")
        }

        private val pluginVersion = PluginManagerCore.getPlugin(PluginId.getId("com.vaadin.intellij-plugin"))?.version

        fun isVaadinProject(project: Project): Boolean {
            var isVaadinProject = false
            for (module in ModuleManager.getInstance(project).modules) {
                ModuleRootManager.getInstance(module).orderEntries().forEachLibrary { library: Library ->
                    if (!isVaadinProject && library.name?.contains("com.vaadin") == true) {
                        isVaadinProject = true
                    }
                    true
                }
            }
            return isVaadinProject
        }

        fun notify(content: String, type: NotificationType) {
            notify(content, type, null)
        }

        fun notify(content: String, type: NotificationType, project: Project?) {
            Notifications.Bus.notify(Notification("Copilot", content, type), project)
        }

        fun isServerRunning(project: Project): Boolean {
            return project.service<CopilotServerService>().isRunning()
        }

        fun startServer(project: Project) {
            val server = project.service<CopilotServerService>()
            if (server.isRunning()) {
                notify("Copilot plugin already started", NotificationType.INFORMATION)
                return
            }
            server.init()
            savePortInDotFile(project, server.getPort()!!)
            ApplicationManager.getApplication().executeOnPooledThread {
                notify("Copilot plugin Started", NotificationType.INFORMATION)
                server.start { data ->
                    handleClientData(project, data)
                }
            }
        }

        fun stopServer(project: Project) {
            val server = project.service<CopilotServerService>()
            if (!server.isRunning()) {
                notify("Copilot plugin is not running", NotificationType.INFORMATION)
                return
            }
            removeDotFile(project)
            server.stop()
            notify("Copilot plugin Stopped", NotificationType.INFORMATION)
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
                HANDLERS.UNDO.command -> return UndoHandler(project)
                HANDLERS.REDO.command -> return RedoHandler(project)
                else -> {
                    LOG.warn("Command $command not supported by plugin")
                }
            }
            return null
        }

        private fun savePortInDotFile(project: Project, port: Int) {
            val baseDirectory = getBasePathDirectory(project)
            if (baseDirectory != null) {
                val props = Properties()
                props.setProperty("port", port.toString())
                props.setProperty("ide", "intellij")
                props.setProperty("version", pluginVersion)
                props.setProperty("supportedActions", HANDLERS.values().map { a -> a.command }.joinToString(","))

                val stringWriter = StringWriter()
                props.store(stringWriter, "Copilot Plugin Runtime Properties")

                val fileType = FileTypeManager.getInstance().getStdFileType("properties")
                runInEdt {
                    ApplicationManager.getApplication().runWriteAction {
                        baseDirectory.findFile(DOTFILE)?.delete()
                        val file = PsiFileFactory.getInstance(project)
                            .createFileFromText(DOTFILE, fileType, stringWriter.toString())
                        baseDirectory.add(file)
                    }
                }
            }
        }

        private fun removeDotFile(project: Project) {
            ApplicationManager.getApplication().runWriteAction {
                getBasePathDirectory(project)?.findFile(DOTFILE)?.delete()
            }
        }

        private fun getBasePathDirectory(project: Project): PsiDirectory? {
            return ApplicationManager.getApplication().runReadAction<PsiDirectory?> {
                val basePath = project.basePath
                if (basePath != null) {
                    val virtualFile = VfsUtil.findFileByIoFile(File(basePath), false)
                    if (virtualFile != null) {
                        return@runReadAction PsiManager.getInstance(project).findDirectory(virtualFile)
                    }
                }
                return@runReadAction null
            }
        }

    }


}