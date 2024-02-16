package com.vaadin.plugin.copilot.service

import com.intellij.openapi.Disposable

interface CopilotServerService : Disposable {

    // binds to port
    fun init()

    fun start(dataReceivedCallback: (ByteArray) -> Unit)

    fun isRunning(): Boolean

    // not empty after running init()
    fun getPort(): Int?

    fun stop()

}
