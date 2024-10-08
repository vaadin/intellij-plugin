package com.vaadin.plugin.hotswapagent

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configurations.JavaCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager

class HotswapAgentRunner : GenericDebuggerRunner() {

    override fun getRunnerId(): String {
        return "vaadin-hotswapagent-runner"
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId.equals(HotswapAgentExecutor.ID)
    }

    override fun execute(environment: ExecutionEnvironment) {
        val runProfile =
            environment.runProfile as? JavaRunConfigurationBase
                ?: throw IllegalStateException("$runnerId can only run Java configurations")
        val javaCommandLine =
            environment.state as? JavaCommandLine ?: throw IllegalStateException("$runnerId needs a JavaCommandLine")
        val module = runProfile.configurationModule?.module ?: throw IllegalStateException("$runnerId needs a module")

        val javaParameters = javaCommandLine.javaParameters
        try {
            val jdkOk =
                JdkUtil.isJetbrainsRuntime(javaParameters.jdk) || JdkUtil.getCompatibleJetbrainsJdk(module) != null

            if (jdkOk) {
                super.execute(environment)
            } else {
                val bundledJetbrainsJdk = JdkUtil.getSdkMajorVersion(JdkUtil.getBundledJetbrainsJdk())
                val projectSdkMajor = JdkUtil.getProjectSdkVersion(module)

                ApplicationManager.getApplication().invokeLater {
                    NoJBRFoundDialog(bundledJetbrainsJdk, projectSdkMajor).show()
                }
            }
        } catch (e: BrokenJbrException) {
            ApplicationManager.getApplication().invokeLater { BadJBRFoundDialog().show() }
        }
    }
}
