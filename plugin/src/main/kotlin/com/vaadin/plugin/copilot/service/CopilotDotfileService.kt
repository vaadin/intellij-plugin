package com.vaadin.plugin.copilot.service

import java.io.IOException
import java.nio.file.Path

interface CopilotDotfileService {

    fun isActive(): Boolean

    @Throws(IOException::class) fun removeDotfile()

    @Throws(IOException::class) fun createDotfile(content: String)

    fun getDotfileDirectory(): Path?

    fun getDotfile(): Path?
}
