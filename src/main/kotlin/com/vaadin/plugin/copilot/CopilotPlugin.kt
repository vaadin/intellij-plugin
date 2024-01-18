package com.vaadin.plugin.copilot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseListener
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.copilot.handlers.UndoHandler
import com.vaadin.plugin.copilot.handlers.WriteHandler
import java.io.File
import java.io.FileWriter
import java.util.*


class CopilotPlugin : ProjectActivity {

    fun interface CommandHandler {
        fun handle()
    }

    data class CommandRequest(val command: String, val data: Map<String, Any>)

    override suspend fun execute(project: Project) {
        notify("Copilot Plugin Started", NotificationType.INFORMATION)

        val server = CopilotServer { data ->
            handleClientData(project, data)
        }
        savePortInDotFile(project, server.getPort())
        project.messageBus.connect().subscribe(ProjectCloseListener.TOPIC, object : ProjectCloseListener {
            override fun projectClosing(project: Project) {
                server.stop()
                super.projectClosing(project)
            }
        })
        server.start()
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

    private fun createHandler(command: String, project: Project, data: Map<String, Any>): CommandHandler? {
        when (command) {
            "write" -> return WriteHandler(project, data)
            "undo" -> return UndoHandler(project)
        }
        return null
    }

    private fun notify(message: String, type: NotificationType) {
        Notifications.Bus.notify(Notification("Copilot", message, type))
    }

    private fun savePortInDotFile(project: Project, port: Int) {
        val ioFile = project.basePath + File.separator + ".copilot-plugin"
        val props = Properties()
        props.setProperty("port", port.toString())
        props.store(FileWriter(ioFile), "Copilot Plugin Runtime Properties")
        File(ioFile).deleteOnExit()
    }

}