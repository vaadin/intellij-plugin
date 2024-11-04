package com.vaadin.plugin.copilot

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher

class DefaultProgramPatcher : JavaProgramPatcher() {

    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {

        if (javaParameters == null) {
            return
        }
        val paramsList = javaParameters.vmParametersList

        if (runProfile is RunConfiguration) {
            CopilotPluginUtil.getDotFile(runProfile.project)?.let {
                paramsList.add("-Dvaadin.copilot.pluginDotFilePath=${it.path}")
            }
        }
    }
}
