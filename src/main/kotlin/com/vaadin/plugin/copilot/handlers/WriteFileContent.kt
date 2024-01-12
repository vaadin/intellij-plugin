package com.vaadin.plugin.copilot.handlers

import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.CopilotServer
import java.io.File
import java.io.FileWriter

class WriteFileContent(val project: Project): CopilotServer.CommandHandler {

    override fun handle(data: Map<String, Object>): CopilotServer.CommandResponse {
        val file = project.basePath + File.separator + data["file"]
        val fw = FileWriter(file)
        fw.write(data["content"] as String)
        fw.flush()
        return CopilotServer.CommandResponse(true)
    }

}