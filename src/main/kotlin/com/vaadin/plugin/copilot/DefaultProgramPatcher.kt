package com.vaadin.plugin.copilot

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class DefaultProgramPatcher : JavaProgramPatcher() {

    private val LOG: Logger = Logger.getInstance(DefaultProgramPatcher::class.java)

    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {

        // Create plugin present property -DcopilotPluginActive=true
        if (javaParameters == null) {
            return
        }
        val paramsList = javaParameters.vmParametersList

        if (runProfile is RunConfiguration) {
            val project: Project = runProfile.project
            val projectBasePath: String? = project.basePath

            projectBasePath?.let {
                val dotFilePath = CopilotPluginUtil.getDotFilePath(project)
                if (dotFilePath != null) {
                    paramsList.add("-DcopilotPluginPath=$dotFilePath")
                }
            }
        }
    }
}
