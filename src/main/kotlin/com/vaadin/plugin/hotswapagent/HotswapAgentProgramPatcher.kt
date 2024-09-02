package com.vaadin.plugin.hotswapagent

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.ProjectRootManager
import org.apache.commons.io.IOUtils
import org.jetbrains.jps.model.java.JdkVersionDetector
import java.io.File
import java.net.URL


class HotswapAgentProgramPatcher : JavaProgramPatcher() {
    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {
        if (executor !is HotswapAgentExecutor) {
            return;
        }
        if (runProfile !is RunConfiguration) {
            return;
        }

        var homePath = javaParameters?.jdk?.homePath ?: throw IllegalStateException("No JDK found");
        if (!isJetbrainsRuntime(homePath)) {
            // See if we can use the bundled Jetbrains Runtime
            val jbrHomePath = PathManager.getBundledRuntimePath()
            val jbrSdk = JavaSdk.getInstance().createJdk("Bundled JBR", jbrHomePath, false);
            val projectSdk = ProjectRootManager.getInstance(runProfile.project).projectSdk // FIXME This should compare to project java level, not jdk version
                ?: throw IllegalArgumentException("Unable to find a project JDK");

            var bundledSdkVersion = JavaSdk.getInstance().getVersion(jbrSdk)?: throw IllegalArgumentException("Unable to detect bundled sdk version");
            var projectSdkVersion = JavaSdk.getInstance().getVersion(projectSdk) ?: throw IllegalArgumentException("Unable to detect project sdk version");

            if (true ||bundledSdkVersion.maxLanguageLevel.isAtLeast(projectSdkVersion.maxLanguageLevel)) {
                javaParameters.jdk = jbrSdk;
                homePath = jbrHomePath;
            }else {
                throw IllegalArgumentException("The bundled JBR is not compatible with the project JDK");
            }
        }
        val agentInJdk = File(homePath, "lib/hotswap/hotswap-agent.jar");
        if (!agentInJdk.exists()) {
            // Try to copy the agent to the JDK
            val bundledHotswap: URL = this::class.java.classLoader.getResource("hotswap-agent.jar")
                ?: throw IllegalStateException("The plugin package is broken: no hotswap-agent.jar found");
            if (!agentInJdk.parentFile.mkdirs()) {
                throw IllegalStateException("Unable to create directory for hotswap-agent.jar");
            }
            try {
                IOUtils.copyLarge(bundledHotswap.openStream(), agentInJdk.outputStream());
            } catch (e: Exception) {
                throw IllegalStateException("Unable to copy hotswap-agent.jar to JDK", e);
            }
        }

        val paramsList = javaParameters.vmParametersList;
        paramsList.add("--add-opens java.base/sun.nio.ch=ALL-UNNAMED");
        paramsList.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
        paramsList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
        paramsList.add("--add-opens=java.base/java.io=ALL-UNNAMED");

        paramsList.add("-XX:+AllowEnhancedClassRedefinition");
        paramsList.add("-XX:HotswapAgent=fatjar");
    }

    private fun isJetbrainsRuntime(homePath: String): Boolean {
        val jdkInfo = JdkVersionDetector.getInstance().detectJdkVersionInfo(homePath);
        return "JBR" == jdkInfo?.variant?.prefix;
    }
}