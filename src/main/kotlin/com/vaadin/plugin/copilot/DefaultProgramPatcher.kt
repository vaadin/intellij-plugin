package com.vaadin.plugin.copilot

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.diagnostic.Logger

class DefaultProgramPatcher : JavaProgramPatcher() {

    private val LOG: Logger = Logger.getInstance(DefaultProgramPatcher::class.java)

    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {

        // Create plugin present property -DcopilotPluginActive=true
        if (javaParameters == null) {
            return
        }
        val paramsList = javaParameters.vmParametersList

        if (runProfile is RunConfiguration) {
            CopilotPluginUtil.getDotFile(runProfile.project)?.let { paramsList.add("-DcopilotPluginPath=${it.path}") }
        }
    }
}
