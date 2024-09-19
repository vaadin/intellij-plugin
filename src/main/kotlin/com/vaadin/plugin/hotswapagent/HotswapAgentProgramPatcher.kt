package com.vaadin.plugin.hotswapagent

import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.diagnostic.Logger
import com.vaadin.plugin.utils.VaadinHomeUtil


class HotswapAgentProgramPatcher : JavaProgramPatcher() {

    private val LOG: Logger = Logger.getInstance(HotswapAgentProgramPatcher::class.java)

    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {
        if (executor !is HotswapAgentExecutor) {
            return
        }
        if (runProfile !is JavaRunConfigurationBase) {
            return
        }
        if (javaParameters == null) {
            return
        }
        val module = runProfile.configurationModule?.module ?: return

        if (runProfile.javaClass.simpleName == "SpringBootApplicationRunConfiguration") {
            turnOffFrameDeactivationPolicy(runProfile);
        }
        if (!JdkUtil.isJetbrainsRuntime(javaParameters.jdk)) {
            // Use the bundled Jetbrains Runtime
            javaParameters.jdk = JdkUtil.getCompatibleJetbrainsJdk(module)
                ?: throw IllegalArgumentException("The bundled JBR is not compatible with the project JDK")
        }
        val agentInHome = VaadinHomeUtil.getHotSwapAgentJar()

        val paramsList = javaParameters.vmParametersList

        val addOpens = "--add-opens"
        paramsList.add(addOpens)
        paramsList.add("java.base/sun.nio.ch=ALL-UNNAMED")
        paramsList.add(addOpens)
        paramsList.add("java.base/java.lang=ALL-UNNAMED")
        paramsList.add(addOpens)
        paramsList.add("java.base/java.lang.reflect=ALL-UNNAMED")
        paramsList.add(addOpens)
        paramsList.add("java.base/java.io=ALL-UNNAMED")
        paramsList.add(addOpens)
        paramsList.add("java.base/sun.security.action=ALL-UNNAMED")
        paramsList.add(addOpens)
        paramsList.add("java.desktop/java.beans=ALL-UNNAMED")

        paramsList.add("-XX:+AllowEnhancedClassRedefinition")
        paramsList.add("-XX:+ClassUnloading")

        paramsList.add("-javaagent:$agentInHome")
    }

    private fun turnOffFrameDeactivationPolicy(runProfile: JavaRunConfigurationBase) {
        try {
            val getOptions = runProfile.javaClass.getDeclaredMethod("getOptions")
            getOptions.trySetAccessible();
            var options = getOptions.invoke(runProfile);
            var policy = options.javaClass.getDeclaredField("frameDeactivationUpdatePolicy\$delegate")
            policy.trySetAccessible();

            val prop = policy.get(options);

            prop.javaClass.getMethod("parseAndSetValue", String::class.java).invoke(prop, null);
        } catch (e: Exception) {
            LOG.debug("Failed to turn off frame deactivation policy", e)
        }
    }
}

