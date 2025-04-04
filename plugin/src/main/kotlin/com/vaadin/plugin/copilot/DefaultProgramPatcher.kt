package com.vaadin.plugin.copilot

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.vaadin.plugin.copilot.service.CopilotDotfileService

class DefaultProgramPatcher : JavaProgramPatcher() {

    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {

        if (javaParameters == null) {
            return
        }
        val paramsList = javaParameters.vmParametersList

        if (runProfile is RunConfiguration) {
            runProfile.project.getService(CopilotDotfileService::class.java).getDotfilePath()?.let {
                paramsList.add("-Dvaadin.copilot.pluginDotFilePath=${it}")
            }
        }
    }
}
