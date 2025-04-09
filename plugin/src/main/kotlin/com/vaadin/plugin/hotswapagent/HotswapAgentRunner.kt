package com.vaadin.plugin.hotswapagent

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configurations.JavaCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.NOTIFICATION_GROUP
import com.vaadin.plugin.utils.trackDebugWithHotswap

class HotswapAgentRunner : GenericDebuggerRunner() {

    override fun getRunnerId(): String {
        return "vaadin-hotswapagent-runner"
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId.equals(HotswapAgentExecutor.ID)
    }

    override fun execute(environment: ExecutionEnvironment) {

        if (environment.runProfile !is JavaRunConfigurationBase) {
            Notification(
                    NOTIFICATION_GROUP,
                    "To launch, open the Spring Boot application class and press \"Debug using Hotswap Agent\". " +
                        "Do not launch through Maven or Gradle.",
                    NotificationType.WARNING)
                .setTitle("Only Spring Boot applications are supported")
                .notify(environment.project)
            return
        }

        val runProfile = environment.runProfile as JavaRunConfigurationBase
        val javaCommandLine =
            environment.state as? JavaCommandLine ?: throw IllegalStateException("$runnerId needs a JavaCommandLine")
        val module = runProfile.configurationModule?.module ?: throw IllegalStateException("$runnerId needs a module")

        val javaParameters = javaCommandLine.javaParameters
        try {
            val jdkOk =
                JdkUtil.isJetbrainsRuntime(javaParameters.jdk) || JdkUtil.getCompatibleJetbrainsJdk(module) != null

            if (jdkOk) {
                trackDebugWithHotswap()
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
