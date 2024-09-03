package com.vaadin.plugin.hotswapagent

import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.externalSystem.JavaModuleData
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ProjectRootManager
import com.vaadin.plugin.utils.VaadinHomeUtil
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.jps.model.java.JdkVersionDetector
import org.jetbrains.plugins.gradle.util.GradleUtil


class HotswapAgentProgramPatcher : JavaProgramPatcher() {
    override fun patchJavaParameters(executor: Executor?, runProfile: RunProfile?, javaParameters: JavaParameters?) {
        if (executor !is HotswapAgentExecutor) {
            return;
        }
        if (runProfile !is JavaRunConfigurationBase) {
            return;
        }
        val module = runProfile.configurationModule?.module ?: return;

        var homePath = javaParameters?.jdk?.homePath ?: throw IllegalStateException("No JDK found");
        if (!isJetbrainsRuntime(homePath)) {
            // See if we can use the bundled Jetbrains Runtime
            val jbrHomePath = PathManager.getBundledRuntimePath()
            val jbrSdk = JavaSdk.getInstance().createJdk("Bundled JBR", jbrHomePath, false);
            val projectJavaVersion = getProjectJavaVersion(module);

            var bundledSdkVersion = JavaSdk.getInstance().getVersion(jbrSdk)
                ?: throw IllegalArgumentException("Unable to detect bundled sdk version");

            if (projectJavaVersion == null || bundledSdkVersion.maxLanguageLevel.toJavaVersion()
                    .isAtLeast(projectJavaVersion)
            ) {
                javaParameters.jdk = jbrSdk;
            } else {
                throw IllegalArgumentException("The bundled JBR is not compatible with the project JDK");
            }
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

    private fun getMavenJavaVersion(module: Module): Int? {
        val mavenProject = MavenProjectsManager.getInstance(module.project).findProject(module) ?: return null;
        val target = mavenProject.properties.getProperty("maven.compiler.release")
            ?: mavenProject.properties.getProperty("maven.compiler.target") ?: "17";
        return target.toInt();
    }

    private fun getGradleJavaVersion(module: Module): Int? {
        val gradleModuleData = GradleUtil.findGradleModuleData(module) ?: return null;
        val javaModuleData = ExternalSystemApiUtil.find(
            gradleModuleData,
            Key.create<JavaModuleData>(JavaModuleData::class.java, 1)
        )?.data ?: return null;

        return javaModuleData.targetBytecodeVersion?.toInt();
    }

    private fun getProjectJavaVersion(module: Module): Int? {
        // If a target version is specified in Maven or Gradle, that defines the version needed to run the app.
        // Otherwise, it should be the project SDK version
        return getMavenJavaVersion(module) ?: getGradleJavaVersion(module) ?: getProjectSdkVersion(module);
    }

    private fun getProjectSdkVersion(module: Module): Int? {
        val projectSdk = ProjectRootManager.getInstance(module.project)?.projectSdk ?: return null;
        val projectSdkVersion = JavaSdk.getInstance().getVersion(projectSdk) ?: return null;
        return projectSdkVersion.maxLanguageLevel.toJavaVersion().feature;
    }

    private fun isJetbrainsRuntime(homePath: String): Boolean {
        val jdkInfo = JdkVersionDetector.getInstance().detectJdkVersionInfo(homePath);
        return "JBR" == jdkInfo?.variant?.prefix;
    }
}

