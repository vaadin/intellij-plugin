package com.vaadin.plugin.copilot

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.vaadin.plugin.copilot.handler.*
import com.vaadin.plugin.utils.VaadinIcons
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.BufferedWriter
import java.io.File
import java.io.StringWriter
import java.util.*

class CopilotPluginUtil {

    companion object {

        private val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

        private const val DOTFILE = ".copilot-plugin"

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

                val fileType = FileTypeManager.getInstance().getStdFileType("properties")
                runInEdt {
                    ApplicationManager.getApplication().runWriteAction {
                        dotFileDirectory.findFile(DOTFILE)?.delete()
                        val file =
                            PsiFileFactory.getInstance(project)
                                .createFileFromText(DOTFILE, fileType, stringWriter.toString())
                        dotFileDirectory.add(file)
                        LOG.info("$DOTFILE created in ${dotFileDirectory.virtualFile.path}")
                    }
                }
            } else {
                LOG.error("Cannot create $DOTFILE")
            }
        }

        fun removeDotFile(project: Project) {
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
                VfsUtil.createDirectoryIfMissing(ideaDir)?.let { LOG.info("$ideaDir created") }
            }
        }
    }
}
