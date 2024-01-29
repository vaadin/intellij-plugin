package com.vaadin.plugin.copilot

interface CopilotServerService {

    fun start(dataReceivedCallback: (ByteArray) -> Unit)

    fun isRunning(): Boolean

    fun getPort(): Int

    fun stop()

}