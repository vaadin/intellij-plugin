package com.vaadin.plugin.copilot

interface CopilotServerService {

    // binds to port
    fun init()

    fun start(dataReceivedCallback: (ByteArray) -> Unit)

    fun isRunning(): Boolean

    // not empty after running init()
    fun getPort(): Int?

    fun stop()

}