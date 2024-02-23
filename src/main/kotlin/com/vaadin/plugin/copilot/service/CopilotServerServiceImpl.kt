package com.vaadin.plugin.copilot.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.CopilotPluginUtil
import io.ktor.util.network.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*

// Server implementation based on https://github.com/teocci/NioSocketCodeSample/tree/master
class CopilotServerServiceImpl(private val project: Project): CopilotServerService {

    private val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

    private val timeout: Long = 500

    private var serverChannel: ServerSocketChannel? = null
    private var selector: Selector? = null

    private var serverRunning = false

    override fun getPort(): Int? {
        return serverChannel?.socket()?.localPort
    }

    override fun stop() {
        serverRunning = false
    }

    override fun dispose() {
        if (serverRunning) {
            stop()
        }
    }

    override fun isRunning(): Boolean {
        return serverRunning
    }

    override fun init() {
        try {
            serverChannel = ServerSocketChannel.open()
            serverChannel!!.configureBlocking(false)
            serverChannel!!.socket().bind(null)
            selector = Selector.open()
            serverChannel!!.register(selector, SelectionKey.OP_ACCEPT)
            serverRunning = true
        } catch (e: IOException) {
            LOG.error(e)
        }
    }

    override fun start(dataReceivedCallback: (ByteArray) -> Unit) {
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
            LOG.error(e)
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
                LOG.error(e)
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
        val baos = ByteArrayOutputStream()
        var read: Int
        try {
            while(true) {
                readBuffer.clear()
                read = channel.read(readBuffer)
                if (read == -1) {
                    channel.close()
                    key.cancel()
                    break
                }
                baos.write(readBuffer.array(), 0, read)
            }
        } catch (e: IOException) {
            LOG.error(e)
            key.cancel()
            channel.close()
            return null
        }

        return baos.toByteArray()
    }

}
