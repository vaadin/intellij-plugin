package com.vaadin.plugin.copilot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.handlers.UndoHandler
import com.vaadin.plugin.copilot.handlers.WriteHandler
import java.io.File
import java.io.FileWriter
import java.util.*

class CopilotServerProjectAction : AnAction() {

    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project
        if (project == null) {
            notify("Copilot Plugin can be started only on existing project", NotificationType.WARNING)
            return
        }

        val psiDirectoryNode = p0.dataContext.getData(CommonDataKeys.NAVIGATABLE_ARRAY)!!.first() as PsiDirectoryNode

        val server = project.service<CopilotServerService>()
        if (server.isRunning()) {
            server.stop()
            removeDotFile(psiDirectoryNode)
            notify("Copilot Plugin Stopped", NotificationType.INFORMATION)
            return
        }

        server.init()
        savePortInDotFile(psiDirectoryNode, server.getPort()!!)
        ApplicationManager.getApplication().executeOnPooledThread {
            notify("Copilot Plugin Started", NotificationType.INFORMATION)
            server.start { data ->
                handleClientData(project, data)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY)?.first() is PsiDirectoryNode

        if (e.project?.service<CopilotServerService>()?.isRunning() == true) {
            e.presentation.text = "Stop Copilot Plugin"
        } else {
            e.presentation.text = "Start Copilot Plugin Here"
        }
    }

    private fun handleClientData(project: Project, data: ByteArray) {
        val command: CommandRequest = jacksonObjectMapper().readValue(data)
        println("Running action " + command.command)
        ApplicationManager.getApplication().executeOnPooledThread {
            runInEdt {
                val handler = createHandler(command.command, project, command.data)
                if (handler !== null) {
                    CommandProcessor.getInstance().executeCommand(project, {
                        handler.handle()
                        if (handler is UndoableAction) {
                            UndoManager.getInstance(project).undoableActionPerformed(handler)
                        }
                    }, "copilot-" + command.command, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
                }
            }
        }
    }

    private fun createHandler(
        command: String,
        project: Project,
        data: Map<String, Any>
    ): CommandHandler? {
        when (command) {
            "write" -> return WriteHandler(project, data)
            "undo" -> return UndoHandler(project)
        }
        return null
    }

    private fun notify(message: String, type: NotificationType) {
        Notifications.Bus.notify(Notification("Copilot", message, type))
    }

    private fun savePortInDotFile(psiDirectoryNode: PsiDirectoryNode, port: Int) {
        if (psiDirectoryNode.virtualFile != null) {
            val ioFile = File(psiDirectoryNode.virtualFile!!.canonicalPath + File.separator + ".copilot-plugin")
            val props = Properties()
            props.setProperty("port", port.toString())
            props.setProperty("ide", "intellij")
            props.setProperty("version", "1.0.0")
            props.store(FileWriter(ioFile), "Copilot Plugin Runtime Properties")
        }
    }

    private fun removeDotFile(psiDirectoryNode: PsiDirectoryNode) {
        if (psiDirectoryNode.virtualFile != null) {
            val ioFile = File(psiDirectoryNode.virtualFile!!.canonicalPath + File.separator + ".copilot-plugin")
            ioFile.delete()
        }
    }

}