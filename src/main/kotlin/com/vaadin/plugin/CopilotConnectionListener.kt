package com.vaadin.`intellij-plugin`

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class CopilotConnectionListener : StartupActivity {

    private val LOG: Logger = Logger.getInstance(CopilotConnectionListener::class.java)

    override fun runActivity(project: Project) {
        LOG.info("Copilot Starting Up...")
    }

}