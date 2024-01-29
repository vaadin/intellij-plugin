package com.vaadin.plugin.copilot

import com.intellij.openapi.project.Project
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

// Server implementation based on https://github.com/teocci/NioSocketCodeSample/tree/master
class CopilotServerServiceImpl(private val project: Project): CopilotServerService {

    private val timeout: Long = 500

    private var serverChannel: ServerSocketChannel? = null
    private var selector: Selector? = null

    private var serverRunning = false

    override fun getPort(): Int {
        return serverChannel!!.socket().localPort
    }

    override fun stop() {
        serverRunning = false
        removeDotFile()
    }

    override fun isRunning(): Boolean {
        return serverRunning
    }

    override fun start(dataReceivedCallback: (ByteArray) -> Unit) {

        try {
            serverChannel = ServerSocketChannel.open()
            serverChannel!!.configureBlocking(false)
            serverChannel!!.socket().bind(null)
            selector = Selector.open()
            serverChannel!!.register(selector, SelectionKey.OP_ACCEPT)
            serverRunning = true
        } catch (e: IOException) {
            e.printStackTrace()
        }

        savePortInDotFile(getPort())
        try {
            while (!Thread.currentThread().isInterrupted && serverRunning) {
                selector!!.select(timeout)
                val keys = selector!!.selectedKeys().iterator()

                while (keys.hasNext()) {
                    val key = keys.next()
                    keys.remove()
                    if (!key.isValid) {
                        continue
                    }
                    if (key.isAcceptable) {
                        accept(key)
                    }
//                    if (key.isWritable) {
//                        println("Writing...")
//                        write(key)
//                    }
                    if (key.isReadable) {
                        val data = read(key)
                        if (data != null) {
                            dataReceivedCallback.invoke(data)
                        }
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
    private fun read(key: SelectionKey): ByteArray? {
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
            return null
        }

        if (read == -1) {
            channel.close()
            key.cancel()
            return null
        }
        // IMPORTANT - don't forget the flip() the buffer. It is like a reset without clearing it.
        readBuffer.flip()
        val data = ByteArray(4096)
        readBuffer[data, 0, read]
        return data
    }

    private fun savePortInDotFile(port: Int) {
        val ioFile = File(project.basePath + File.separator + ".copilot-plugin")
        val props = Properties()
        props.setProperty("port", port.toString())
        props.store(FileWriter(ioFile), "Copilot Plugin Runtime Properties")
    }

    private fun removeDotFile() {
        val ioFile = File(project.basePath + File.separator + ".copilot-plugin")
        ioFile.delete()
    }

}