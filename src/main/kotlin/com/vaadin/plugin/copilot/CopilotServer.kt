package com.vaadin.plugin.copilot

import io.ktor.util.network.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

// Server implementation based on https://github.com/teocci/NioSocketCodeSample/tree/master
class CopilotServer(private val dataReceivedCallback: (ByteArray) -> Unit = { }) {

    private val TIMEOUT: Long = 500

    private var serverChannel: ServerSocketChannel? = null
    private var selector: Selector? = null

    private var serverRunning = AtomicBoolean(true)

    init {
//        println("initializing server")

        try {
            serverChannel = ServerSocketChannel.open()
            serverChannel!!.configureBlocking(false)
            serverChannel!!.socket().bind(null)
            selector = Selector.open()
            serverChannel!!.register(selector, SelectionKey.OP_ACCEPT)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getPort(): Int {
        return serverChannel!!.socket().localPort
    }

    fun stop() {
        serverRunning.set(false)
    }

    fun start() {
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
//                        println("Accepting connection")
                        accept(key)
                    }
//                    if (key.isWritable) {
//                        println("Writing...")
//                        write(key)
//                    }
                    if (key.isReadable) {
//                        println("Reading connection")
                        read(key)
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
//        println("Closing server down")
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
    private fun read(key: SelectionKey) {
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
//            println("Nothing was there to be read, closing connection")
            channel.close()
            key.cancel()
            return
        }
        // IMPORTANT - don't forget the flip() the buffer. It is like a reset without clearing it.
        readBuffer.flip()
        val data = ByteArray(4096)
        readBuffer[data, 0, read]

        dataReceivedCallback.invoke(data)
    }

}