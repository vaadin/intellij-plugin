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
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.copilot.handlers.UndoHandler
import com.vaadin.plugin.copilot.handlers.WriteHandler
import io.ktor.util.network.*
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*


class CopilotServer : ProjectActivity {

    fun interface CommandHandler {
        fun handle()
    }

    data class CommandRequest(val command: String, val data: Map<String, Any>)

    override suspend fun execute(project: Project) {
        try {
            val server = ServerSocketChannel.open().bind(null)
            notify("Copilot Plugin Started", NotificationType.INFORMATION)
            saveInDotFile(project, server.localAddress.port)

            while (true) {
                val client: SocketChannel = server.accept()
                if (client.isOpen) {
                    this.handleClientConnection(project, client)
                }
                client.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClientConnection(project: Project, client: SocketChannel) {
        val buffer: ByteBuffer = ByteBuffer.allocate(2048)
        client.read(buffer)

        val data: String = String(buffer.array()).trim { it <= ' ' }
        val command: CommandRequest = jacksonObjectMapper().readValue(data)

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

    private fun saveInDotFile(project: Project, port: Int) {
        val ioFile = project.basePath + File.separator + ".copilot-plugin"
        val props = Properties()
        props.setProperty("port", port.toString())
        props.store(FileWriter(ioFile), "Copilot Plugin Runtime Properties")
    }

}