package com.vaadin.plugin.hotswapagent

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import org.apache.commons.io.IOUtils
import java.io.File

class HotswapAgentProgramPatcher : JavaProgramPatcher() {
    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {
        if (executor !is HotswapAgentExecutor) {
            return;
        }

        val homePath = javaParameters?.jdk?.homePath;
        if (homePath == null) {
            return;
        }

        var agentInJdk = File(homePath, "lib/hotswap/hotswap-agent.jar");
        if (!agentInJdk.exists()) {
            // Try to copy the agent to the JDK
            var bundledHotswap = this::class.java.classLoader.getResource("hotswap-agent.jar");
            if (bundledHotswap == null) {
                return;
            }
            IOUtils.copyLarge(bundledHotswap.openStream(), agentInJdk.outputStream());

        }

        javaParameters.vmParametersList?.add("-javaagent:/path/to/hotswap-agent.jar")
    }
}