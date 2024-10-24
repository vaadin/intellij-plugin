package com.vaadin.plugin.hotswapagent

import com.intellij.externalSystem.JavaModuleData
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import java.io.File
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.jps.model.java.JdkVersionDetector
import org.jetbrains.plugins.gradle.util.GradleUtil

class JdkUtil {

    companion object {
        fun isJetbrainsRuntime(jdk: Sdk?): Boolean {
            val homePath = jdk?.homePath ?: throw IllegalStateException("JDK has no home path: $jdk")
            val jdkInfo = JdkVersionDetector.getInstance().detectJdkVersionInfo(homePath)
            return "jbr" == jdkInfo?.variant?.prefix?.lowercase()
        }

        fun getBundledJetbrainsJdk(): Sdk {
            val jbrHomePath = PathManager.getBundledRuntimePath()
            return JavaSdk.getInstance().createJdk("Bundled JBR", jbrHomePath, false)
        }

        fun getCompatibleJetbrainsJdk(module: Module): Sdk? {
            val projectJavaVersion = getProjectJavaVersion(module)
            val jbrSdk = getBundledJetbrainsJdk()
            val bundledSdkVersion =
                JavaSdk.getInstance().getVersion(jbrSdk)
                    ?: throw IllegalArgumentException("Unable to detect bundled sdk version")

            if (isBrokenJbr(jbrSdk)) {
                throw BrokenJbrException()
            }

            if (projectJavaVersion == null ||
                bundledSdkVersion.maxLanguageLevel.toJavaVersion().isAtLeast(projectJavaVersion)) {
                return jbrSdk
            }
            return null
        }

        private fun isBrokenJbr(sdk: Sdk): Boolean {
            val release = File(sdk.homePath, "release")
            val version = sdk.versionString
            if (version != null && version.contains("21.0.4") && release.exists()) {
                // Find the line with JAVA_RUNTIME_VERSION and check if it's the broken version
                val runtimeVersion = release.readLines().find { it.startsWith("JAVA_RUNTIME_VERSION") }
                if (runtimeVersion != null &&
                    runtimeVersion.contains("21.0.4+13") &&
                    runtimeVersion.contains("b509.17")) {
                    return true
                }
            }

            return false
        }

        private fun getProjectJavaVersion(module: Module): Int? {
            // If a target version is specified in Maven or Gradle, that defines the version needed
            // to run the app.
            // Otherwise, it should be the project SDK version
            return getMavenJavaVersion(module) ?: getGradleJavaVersion(module) ?: getProjectSdkVersion(module)
        }

        private fun getMavenJavaVersion(module: Module): Int? {
            val mavenProject = MavenProjectsManager.getInstance(module.project).findProject(module) ?: return null
            val target =
                mavenProject.properties.getProperty("maven.compiler.release")
                    ?: mavenProject.properties.getProperty("maven.compiler.target")
                    ?: "17"
            return target.toInt()
        }

        private fun getGradleJavaVersion(module: Module): Int? {
            val gradleModuleData = GradleUtil.findGradleModuleData(module) ?: return null
            val javaModuleData =
                ExternalSystemApiUtil.find(gradleModuleData, Key.create<JavaModuleData>(JavaModuleData::class.java, 1))
                    ?.data ?: return null

            return javaModuleData.targetBytecodeVersion?.toInt()
        }

        fun getSdkMajorVersion(sdk: Sdk): Int? {
            val sdkVersion = JavaSdk.getInstance().getVersion(sdk) ?: return null
            return sdkVersion.maxLanguageLevel.toJavaVersion().feature
        }

        fun getProjectSdkVersion(module: Module): Int? {
            val projectSdk = ProjectRootManager.getInstance(module.project)?.projectSdk ?: return null
            return getSdkMajorVersion(projectSdk)
        }
    }
}

class BrokenJbrException : Exception() {}
