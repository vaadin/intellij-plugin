package com.vaadin.plugin.hotswapagent

import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.vaadin.plugin.utils.VaadinHomeUtil


class HotswapAgentProgramPatcher : JavaProgramPatcher() {
    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {
        if (executor !is HotswapAgentExecutor) {
            return;
        }
        if (runProfile !is JavaRunConfigurationBase) {
            return;
        }
        if (javaParameters == null) {
            return;
        }
        val module = runProfile.configurationModule?.module ?: return;

        if (!JdkUtil.isJetbrainsRuntime(javaParameters.jdk)) {
            // Use the bundled Jetbrains Runtime
            javaParameters.jdk = JdkUtil.getCompatibleJetbrainsJdk(module)
                ?: throw IllegalArgumentException("The bundled JBR is not compatible with the project JDK");
        }
        val agentInHome = VaadinHomeUtil.getHotswapAgentJar()

        val paramsList = javaParameters.vmParametersList;
        paramsList.add("--add-opens java.base/sun.nio.ch=ALL-UNNAMED");
        paramsList.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
        paramsList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
        paramsList.add("--add-opens=java.base/java.io=ALL-UNNAMED");

        paramsList.add("-XX:+AllowEnhancedClassRedefinition");

        paramsList.add("-javaagent:$agentInHome");
    }

}

