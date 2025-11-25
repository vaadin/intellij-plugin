package com.vaadin.plugin.hotswapagent

import com.intellij.externalSystem.JavaModuleData
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.SdkPopupBuilder
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory
import java.io.File
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.jps.model.java.JdkVersionDetector

class JdkUtil {

    companion object {

        val GRADLE_SYSTEM_ID: ProjectSystemId = ProjectSystemId("GRADLE")

        fun getProjectSdk(project: Project): Sdk? {
            return ProjectRootManager.getInstance(project).projectSdk
        }

        fun isJetbrainsRuntime(jdk: Sdk?): Boolean {
            val homePath = jdk?.homePath ?: throw IllegalStateException("JDK has no home path: $jdk")
            val jdkInfo = JdkVersionDetector.getInstance().detectJdkVersionInfo(homePath)
            return JdkVersionDetector.Variant.JBR == jdkInfo?.variant
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
            return parseJavaVersion(target)
        }

        private fun getGradleJavaVersion(module: Module): Int? {
            val gradleModuleData = findGradleModuleData(module) ?: return null
            val javaModuleData =
                ExternalSystemApiUtil.find(gradleModuleData, Key.create<JavaModuleData>(JavaModuleData::class.java, 1))
                    ?.data ?: return null

            val targetVersion = javaModuleData.targetBytecodeVersion ?: return null
            return parseJavaVersion(targetVersion)
        }

        fun getSdkMajorVersion(sdk: Sdk): Int? {
            val sdkVersion = JavaSdk.getInstance().getVersion(sdk) ?: return null
            return sdkVersion.maxLanguageLevel.toJavaVersion().feature
        }

        fun getProjectSdkVersion(module: Module): Int? {
            val projectSdk = ProjectRootManager.getInstance(module.project)?.projectSdk ?: return null
            return getSdkMajorVersion(projectSdk)
        }

        internal fun parseJavaVersion(version: String): Int? {
            val trimmed = version.trim()

            // Standard single-number versions, e.g. "17"
            trimmed.toIntOrNull()?.let {
                return it
            }

            // Legacy format "1.8"
            if (trimmed.startsWith("1.")) {
                trimmed
                    .substringAfter("1.")
                    .takeWhile { it.isDigit() }
                    .toIntOrNull()
                    ?.let {
                        return it
                    }
            }

            // Fallback for strings like "11.0.4"
            return trimmed.takeWhile { it.isDigit() }.toIntOrNull()
        }

        private fun findGradleModuleData(module: Module): DataNode<ModuleData>? {
            val projectPath = ExternalSystemApiUtil.getExternalProjectPath(module)
            if (projectPath == null) {
                return null
            } else {
                val project: Project = module.project
                return ExternalSystemApiUtil.findModuleNode(project, GRADLE_SYSTEM_ID, projectPath)
            }
        }

        fun createSdkPopupBuilder(project: Project): SdkPopupBuilder {
            return SdkPopupFactory.newBuilder()
                .withProject(project)
                .withSdkFilter(JdkUtil::isJetbrainsRuntime)
                .updateProjectSdkFromSelection()
        }
    }
}

class BrokenJbrException : Exception() {}
