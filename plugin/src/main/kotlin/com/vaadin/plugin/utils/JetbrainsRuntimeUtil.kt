package com.vaadin.plugin.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.vaadin.open.OSUtils
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

const val JETBRAINS_RELEASES_URL = "https://api.github.com/repos/JetBrains/JetBrainsRuntime/releases"

object JetbrainsRuntimeUtil {

    private val LOG: Logger = Logger.getInstance(JetbrainsRuntimeUtil::class.java)

    private const val TAR_GZ = ".tar.gz"

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GitHubRelease(
        val id: Int,
        val name: String,
        val tag_name: String,
        val prerelease: Boolean,
        val body: String
    )

    enum class JBRInstallStatus {
        INSTALLED,
        ALREADY_EXISTS,
        ERROR
    }

    data class JBRInstallResult(val status: JBRInstallStatus, val path: Path?)

    data class JBRSdkInfo(val arch: String, val sdkType: String, val url: String)

    private fun getArchitecture(): String = System.getProperty("os.arch")

    private fun getJavaHome(jdkFolder: Path): Path {
        return if (OSUtils.isMac()) jdkFolder.resolve("Contents").resolve("Home") else jdkFolder
    }

    private fun getJavaHome(jdkFolder: File): File {
        return getJavaHome(jdkFolder.toPath()).toFile()
    }

    fun getHotswapAgentLocation(jdkFolder: File): File {
        return File(File(File(getJavaHome(jdkFolder), "lib"), "hotswap"), "hotswap-agent.jar")
    }

    fun getJavaExecutable(jdkFolder: File): File {
        val bin = if (OSUtils.isWindows()) "java.exe" else "java"
        return File(File(getJavaHome(jdkFolder), "bin"), bin)
    }

    /**
     * Downloads latest JBR if not present
     *
     * @param project current project
     * @return location of downloaded JBR or null on failure
     */
    @Throws(IOException::class, URISyntaxException::class)
    fun downloadLatestJBR(project: Project): CompletableFuture<JBRInstallResult> {
        val latest = findLatestJBRRelease()
        if (latest == null) {
            return CompletableFuture.completedFuture(JBRInstallResult(status = JBRInstallStatus.ERROR, path = null))
        }
        val downloadUrl = findJBRDownloadUrl(latest)
        if (downloadUrl.isPresent) {
            val url = downloadUrl.get()
            val filename = getFilename(url)
            val target = VaadinHomeUtil.resolveVaadinHomeDirectory().resolve("jdk/$filename")
            val outputPath = Path.of(target.path.removeSuffix(TAR_GZ))
            if (outputPath.exists()) {
                return CompletableFuture.completedFuture(
                    JBRInstallResult(status = JBRInstallStatus.ALREADY_EXISTS, path = outputPath))
            }

            val folder = target.parentFile
            if (!folder.exists() && !folder.mkdirs()) {
                throw IOException("Unable to create ${folder.absolutePath}")
            }
            LOG.info("Downloading JetBrains Runtime into ${target.absolutePath}")
            return DownloadUtil.download(project, url.toExternalForm(), target.toPath(), "JetBrains Runtime")
                .thenRun {
                    DownloadUtil.extractTarGz(target, folder)
                    FileUtil.delete(target)
                }
                .thenApply {
                    markAllExecutable(getJavaHome(outputPath).resolve("bin"))
                    JBRInstallResult(status = JBRInstallStatus.INSTALLED, path = outputPath)
                }
        }

        return CompletableFuture.completedFuture(JBRInstallResult(status = JBRInstallStatus.ERROR, path = null))
    }

    fun addAndSetProjectSdk(project: Project, sdkHomePath: String) {
        val sdkHomePath = getJavaHome(File(sdkHomePath)).path
        val sdkType = JavaSdk.getInstance()
        val jdkTable = ProjectJdkTable.getInstance()

        // Check if SDK with same home path already exists
        val existingSdk = jdkTable.allJdks.find { it.homePath == sdkHomePath && it.sdkType == sdkType }
        if (existingSdk == null) {
            val sdk = SdkConfigurationUtil.createAndAddSDK(sdkHomePath, sdkType)
            runWriteAction { ProjectRootManager.getInstance(project).projectSdk = sdk }
            return
        }

        // Check if current SDK is latest JBR
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk
        if (existingSdk.homePath != projectSdk?.homePath) {
            runWriteAction { ProjectRootManager.getInstance(project).projectSdk = existingSdk }
        }
    }

    private fun getFilename(url: URL): String = url.file.replace(".*/".toRegex(), "")

    private fun findJBRDownloadUrl(latest: GitHubRelease): Optional<URL> {
        val sdk = findCorrectReleaseForArchitecture(latest.body)
        return sdk.map { URL(it.url) }
    }

    fun findLatestJBRRelease(): GitHubRelease? {
        try {
            val text = DownloadUtil.openUrlWithProxy("$JETBRAINS_RELEASES_URL/latest")
            return jacksonObjectMapper().readValue(text, GitHubRelease::class.java)
        } catch (e: Exception) {
            throw IOException("Unable to fetch JetBrains Runtime releases info", e)
        }
    }

    private fun findCorrectReleaseForArchitecture(body: String): Optional<JBRSdkInfo> {
        val jbrSdks = findAllJbrSdks(body)
        val key = getDownloadKey()
        return Optional.ofNullable(jbrSdks[key])
    }

    private fun getDownloadKey(): String {
        val jvmArch = getArchitecture()
        val prefix =
            when {
                OSUtils.isMac() -> "osx"
                OSUtils.isWindows() -> "windows"
                else -> "linux"
            }
        val suffix =
            when (jvmArch) {
                "aarch64" -> "aarch64"
                "x86" -> "x86"
                else -> "x64"
            }
        return "$prefix-$suffix"
    }

    private fun findAllJbrSdks(body: String): Map<String, JBRSdkInfo> {
        return body
            .replace("\r", "")
            .split("\n")
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 4) return@mapNotNull null
                val arch = parts[1].trim()
                val sdkType = parts[2].replace("*", "").trim()
                val url = parts[3].replace(Regex("\\[.*]"), "").replace("(", "").replace(")", "").trim()
                JBRSdkInfo(arch, sdkType, url)
            }
            .filter { it.sdkType == "JBRSDK" && it.url.endsWith(TAR_GZ) && !it.url.contains("_diz") }
            .associateBy { it.arch }
    }

    fun markAllExecutable(dir: Path) {
        Files.list(dir).filter { path -> path.isRegularFile() }.forEach { path -> path.toFile().setExecutable(true) }
    }
}
