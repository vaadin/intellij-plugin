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
import io.ktor.util.network.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

// Server implementation based on https://github.com/teocci/NioSocketCodeSample/tree/master
class CopilotServer : ProjectActivity, ProjectCloseListener {

    private val TIMEOUT: Long = 500

    private var serverChannel: ServerSocketChannel? = null
    private var selector: Selector? = null

    private var serverRunning = AtomicBoolean(true)

    private fun init(project: Project) {
        println("initializing server")

        try {
            serverChannel = ServerSocketChannel.open()
            serverChannel!!.configureBlocking(false)
            serverChannel!!.socket().bind(null)

            notify("Copilot Plugin Started", NotificationType.INFORMATION)
            saveInDotFile(project, serverChannel!!.localAddress.port)

            selector = Selector.open()
            serverChannel!!.register(selector, SelectionKey.OP_ACCEPT)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun projectClosing(project: Project) {
        serverRunning.set(false)
        super.projectClosing(project)
    }

    fun interface CommandHandler {
        fun handle()
    }

    data class CommandRequest(val command: String, val data: Map<String, Any>)

    override suspend fun execute(project: Project) {

        init(project)

        project.messageBus.connect().subscribe(ProjectCloseListener.TOPIC, this)

        try {
            while (!Thread.currentThread().isInterrupted && serverRunning.get()) {
                selector!!.select(TIMEOUT)
                val keys = selector!!.selectedKeys().iterator()

                while (keys.hasNext()) {
                    val key = keys.next()
                    keys.remove()
                    if (!key.isValid) {
                        continue
                    }
                    if (key.isAcceptable) {
                        println("Accepting connection")
                        accept(key)
                    }
//                    if (key.isWritable) {
//                        println("Writing...")
//                        write(key)
//                    }
                    if (key.isReadable) {
                        println("Reading connection")
                        read(key, project)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            closeConnection()
        }
    }

    private fun closeConnection() {
        println("Closing server down")
        if (selector != null) {
            try {
                selector!!.close()
                serverChannel!!.socket().close()
                serverChannel!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun accept(key: SelectionKey) {
        val serverSocketChannel = key.channel() as ServerSocketChannel
        val socketChannel = serverSocketChannel.accept()
        socketChannel.configureBlocking(false)
        socketChannel.register(selector, SelectionKey.OP_READ)
    }

    @Throws(IOException::class)
    private fun read(key: SelectionKey, project: Project) {
        val channel = key.channel() as SocketChannel
        val readBuffer = ByteBuffer.allocate(4096)
        readBuffer.clear()

        val read: Int
        try {
            read = channel.read(readBuffer)
        } catch (e: IOException) {
            e.printStackTrace()
            key.cancel()
            channel.close()
            return
        }

        if (read == -1) {
            println("Nothing was there to be read, closing connection")
            channel.close()
            key.cancel()
            return
        }
        // IMPORTANT - don't forget the flip() the buffer. It is like a reset without clearing it.
        readBuffer.flip()
        val data = ByteArray(4096)
        readBuffer[data, 0, read]

        handleClientData(project, data)
    }

    private fun handleClientData(project: Project, data: ByteArray) {
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
        File(ioFile).deleteOnExit()
    }

}