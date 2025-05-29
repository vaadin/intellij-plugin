package com.vaadin.plugin.hotswapagent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.vaadin.open.OSUtils
import com.vaadin.plugin.utils.DownloadUtil
import com.vaadin.plugin.utils.VaadinHomeUtil
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import java.util.Optional

object JetbrainsRuntimeUtil {

    private val LOG: Logger = Logger.getInstance(JetbrainsRuntimeUtil::class.java)

    private const val TAR_GZ = ".tar.gz"
    private const val JETBRAINS_GITHUB_RELEASES_PAGE =
        "https://api.github.com/repos/JetBrains/JetBrainsRuntime/releases"

    @JsonIgnoreProperties(ignoreUnknown = true) data class GitHubReleaseWithBody(val body: String)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GitHubRelease(val id: Int, val name: String, val tag_name: String, val prerelease: Boolean) :
        Comparable<GitHubRelease> {
        override fun compareTo(other: GitHubRelease): Int = compareNumerically(tag_name, other.tag_name)

        private fun compareNumerically(s1: String, s2: String): Int {
            val commonLength = minOf(s1.length, s2.length)
            for (i in 0 until commonLength) {
                val c1 = s1[i]
                val c2 = s2[i]
                if (c1 != c2) {
                    return c1.compareTo(c2)
                }
            }
            return s1.length.compareTo(s2.length)
        }
    }

    data class JBRSdkInfo(val arch: String, val sdkType: String, val url: String)

    private fun getArchitecture(): String = System.getProperty("os.arch")

    private fun getJavaHome(jdkFolder: File): File {
        return if (OSUtils.isMac()) {
            jdkFolder.toPath().resolve("Contents").resolve("Home").toFile()
        } else {
            jdkFolder
        }
    }

    fun getHotswapAgentLocation(jdkFolder: File): File {
        return File(File(File(getJavaHome(jdkFolder), "lib"), "hotswap"), "hotswap-agent.jar")
    }

    fun getJavaExecutable(jdkFolder: File): File {
        val bin = if (OSUtils.isWindows()) "java.exe" else "java"
        return File(File(getJavaHome(jdkFolder), "bin"), bin)
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun downloadLatestJBR(project: Project): Optional<File> {
        val latest = findLatestJBRRelease()
        val downloadUrl = findJBRDownloadUrl(latest)
        return if (downloadUrl.isPresent) {
            val url = downloadUrl.get()
            val filename = getFilename(url)
            val target = VaadinHomeUtil.resolveVaadinHomeDirectory().resolve("jdk/$filename")
            val folder = target.parentFile
            if (!folder.exists() && !folder.mkdirs()) {
                throw IOException("Unable to create ${folder.absolutePath}")
            }
            downloadIfNotPresent(project, url.toString(), target)
            Optional.of(target)
        } else {
            Optional.empty()
        }
    }

    @Throws(URISyntaxException::class)
    private fun downloadIfNotPresent(project: Project, url: String, target: File) {
        if (target.exists() && target.length() > 0) {
            return
        }

        DownloadUtil.download(project, url, target.toPath(), "JetBrains Runtime", true) {
            LOG.info("Downloading JetBrains Runtime into ${target.absolutePath}")
        }
    }

    private fun getFilename(url: URL): String = url.file.replace(".*/".toRegex(), "")

    private fun findJBRDownloadUrl(latest: GitHubRelease): Optional<URL> {
        return try {
            val text =
                DownloadUtil.openUrlWithIntelliJProxy(
                    "https://api.github.com/repos/JetBrains/JetBrainsRuntime/releases/${latest.id}")
            val release = jacksonObjectMapper().readValue(text, GitHubReleaseWithBody::class.java)
            val sdk = findCorrectReleaseForArchitecture(release.body)
            sdk.map { URL(it.url) }
        } catch (e: Exception) {
            LOG.error("Unable to fetch JetBrains Runtime download URL", e)
            Optional.empty()
        }
    }

    @Throws(IOException::class)
    private fun findLatestJBRRelease(): GitHubRelease {
        val typeRef = object : TypeReference<List<GitHubRelease>>() {}
        return try {
            val text = DownloadUtil.openUrlWithIntelliJProxy(JETBRAINS_GITHUB_RELEASES_PAGE)
            val releases = jacksonObjectMapper().readValue(text, typeRef)
            releases.filter { !it.prerelease }.sorted().last()
        } catch (e: Exception) {
            throw IOException("Unable to fetch JetBrains Runtime releases info", e)
        }
    }

    private fun findCorrectReleaseForArchitecture(body: String): Optional<JBRSdkInfo> {
        val jbrSdks = findAllJbrSdks(body)
        val key = getDownloadKey()
        return Optional.ofNullable(jbrSdks[key])
    }

    fun getDownloadKey(): String {
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
}
