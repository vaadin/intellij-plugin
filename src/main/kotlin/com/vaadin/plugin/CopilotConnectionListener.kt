package com.vaadin.plugin

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CopilotConnectionListener : ProjectActivity {

    private val LOG: Logger = Logger.getInstance(CopilotConnectionListener::class.java)

    override suspend fun execute(project: Project) {
        LOG.error("Copilot Starting Up...")
    }

}