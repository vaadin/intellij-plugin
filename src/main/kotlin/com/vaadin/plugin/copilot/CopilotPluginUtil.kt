package com.vaadin.plugin.copilot

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.*
import com.vaadin.plugin.copilot.handler.*
import com.vaadin.plugin.utils.VaadinIcons
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.BufferedWriter
import java.io.IOException
import java.io.StringWriter
import java.util.*

class CopilotPluginUtil {

    companion object {

        private val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

        const val DOTFILE = ".copilot-plugin"

        private const val IDEA_DIR = ".idea"

        private const val NORMALIZED_LINE_SEPARATOR = "\n"

        private const val NOTIFICATION_GROUP = "Vaadin Copilot"

        private enum class HANDLERS(val command: String) {
            WRITE("write"),
            WRITE_BASE64("writeBase64"),
            UNDO("undo"),
            REDO("redo"),
            REFRESH("refresh"),
            SHOW_IN_IDE("showInIde"),
        }

        private val pluginVersion = PluginManagerCore.getPlugin(PluginId.getId("com.vaadin.intellij-plugin"))?.version

        fun getPluginVersion(): String? {
            return pluginVersion
        }

        fun notify(content: String, type: NotificationType, project: Project?) {
            Notifications.Bus.notify(
                Notification(NOTIFICATION_GROUP, content, type).setIcon(VaadinIcons.VAADIN),
                project,
            )
        }

        fun createCommandHandler(command: String, project: Project, data: Map<String, Any>): Handler {
            when (command) {
                HANDLERS.WRITE.command -> return WriteFileHandler(project, data)
                HANDLERS.WRITE_BASE64.command -> return WriteBase64FileHandler(project, data)
                HANDLERS.UNDO.command -> return UndoHandler(project, data)
                HANDLERS.REDO.command -> return RedoHandler(project, data)
                HANDLERS.SHOW_IN_IDE.command -> return ShowInIdeHandler(project, data)
                HANDLERS.REFRESH.command -> return RefreshHandler(project)
                else -> {
                    LOG.warn("Command $command not supported by plugin")
                    return object : Handler {
                        override fun run(): HandlerResponse {
                            return HandlerResponse(HttpResponseStatus.BAD_REQUEST)
                        }
                    }
                }
            }
        }

        fun saveDotFile(project: Project) {
            val dotFileDirectory = getDotFileDirectory(project)
            if (dotFileDirectory != null) {
                val props = Properties()
                props.setProperty("endpoint", RestUtil.getEndpoint())
                props.setProperty("ide", "intellij")
                props.setProperty("version", pluginVersion)
                props.setProperty("supportedActions", HANDLERS.entries.joinToString(",") { a -> a.command })

                val stringWriter = StringWriter()
                val bufferedWriter =
                    object : BufferedWriter(stringWriter) {
                        override fun newLine() {
                            write(NORMALIZED_LINE_SEPARATOR)
                        }
                    }
                props.store(bufferedWriter, "Vaadin Copilot Integration Runtime Properties")
                runInEdt {
                    runWriteAction {
                        val dotFile = dotFileDirectory.findFile(DOTFILE)
                        dotFile?.let {
                            try {
                                it.delete(this)
                            } catch (e: IOException) {
                                LOG.error("Failed to delete $DOTFILE: ${e.message}")
                            }
                        }
                        val newFile =
                            try {
                                dotFileDirectory.createChildData(this, DOTFILE)
                            } catch (e: IOException) {
                                LOG.error("Failed to create $DOTFILE: ${e.message}")
                                return@runWriteAction
                            }

                        try {
                            VfsUtil.saveText(newFile, stringWriter.toString())
                        } catch (e: IOException) {
                            LOG.error("Failed to write to $DOTFILE: ${e.message}")
                        }
                        LOG.info("$newFile created")
                    }
                }
            }
        }

        fun removeDotFile(project: Project) {
            runInEdt {
                runWriteAction {
                    val dotFile = getDotFileDirectory(project)?.findFile(DOTFILE)
                    dotFile?.let {
                        try {
                            it.delete(this)
                            LOG.info("$it removed")
                        } catch (e: IOException) {
                            LOG.error("Failed to delete $DOTFILE: ${e.message}")
                        }
                        return@runWriteAction
                    }
                    LOG.warn("Cannot remove $dotFile - file does not exist")
                }
            }
        }

        fun getDotFileDirectory(project: Project): VirtualFile? {
            val projectDir = project.guessProjectDir()
            if (projectDir == null) {
                LOG.error("Cannot guess project directory")
                return null
            }
            LOG.info("Project directory: $projectDir")
            return projectDir.findOrCreateDirectory(IDEA_DIR)
        }
    }
}
