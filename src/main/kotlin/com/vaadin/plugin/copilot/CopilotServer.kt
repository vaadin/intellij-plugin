package com.vaadin.plugin.copilot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.vaadin.plugin.copilot.handlers.WriteFileContent
import io.ktor.util.network.*
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel


class CopilotServer : ProjectActivity {

    private val handlers: HashMap<String, CommandHandler> = HashMap()

    fun interface CommandHandler {
        fun handle(message: Map<String, Object>): CommandResponse
    }

    data class CommandRequest(val command: String, val data: Map<String, Object>)

    data class CommandResponse(val status: Boolean)

    override suspend fun execute(project: Project) {

        registerHandlers(project)

        try {
            val server = ServerSocketChannel.open().bind(null);
            notify("Copilot Plugin listening at port " + server.localAddress.port, NotificationType.INFORMATION)

            while(true) {
                notify("waiting for client...", NotificationType.INFORMATION)
                val client: SocketChannel = server.accept()
                if ((client != null) && (client.isOpen)) {
                    this.handleClientConnection(client)
                }
                client.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun registerHandlers(project: Project) {
        handlers["write-file"] = WriteFileContent(project)
    }

    fun handleClientConnection(client: SocketChannel) {
        val buffer: ByteBuffer = ByteBuffer.allocate(2048)
        client.read(buffer)

        val data: String = String(buffer.array()).trim { it <= ' ' }
        var command: CommandRequest = jacksonObjectMapper().readValue(data)

        notify("Received from client: " + data, NotificationType.INFORMATION)
        val response = handlers[command.command]?.handle(command.data)
        if (response !== null) {
            buffer.flip()
            buffer.clear()
            client.write(ByteBuffer.wrap(jacksonObjectMapper().writeValueAsBytes(response)))
            notify("Writing back to client: " + response, NotificationType.INFORMATION)
        }
    }

    fun notify(message: String, type: NotificationType) {
        Notifications.Bus.notify(Notification("Copilot", message, type))
    }

}