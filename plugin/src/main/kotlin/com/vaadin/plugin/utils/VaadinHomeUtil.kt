package com.vaadin.plugin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.text.VersionComparatorUtil
import elemental.json.Json
import elemental.json.JsonException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.jar.JarInputStream
import org.jetbrains.annotations.VisibleForTesting

object VaadinHomeUtil {
    private val LOG: Logger = Logger.getInstance(VaadinHomeUtil::class.java)
    private const val PROPERTY_USER_HOME: String = "user.home"
    private const val VAADIN_FOLDER_NAME: String = ".vaadin"
    private const val HOTSWAP_AGENT_JAR_FILE_NAME = "hotswap-agent.jar"

    /** Replaces the Vaadin home directory, so tests don't have to change the JVM-wide `user.home`. */
    @VisibleForTesting internal var vaadinHomeDirectoryOverride: File? = null

    /**
     * Get Vaadin home directory.
     *
     * @return File instance for Vaadin home folder. Does not check if the folder exists.
     */
    fun resolveVaadinHomeDirectory(): File {
        vaadinHomeDirectoryOverride?.let {
            return it
        }
        val userHome = System.getProperty(PROPERTY_USER_HOME)
        return File(userHome, VAADIN_FOLDER_NAME)
    }

    @Throws(IOException::class)
    fun getUserKey(): String {
        val vaadinHome = resolveVaadinHomeDirectory()
        val userKeyFile = File(vaadinHome, "userKey")
        if (userKeyFile.exists()) {
            try {
                val content = Files.readString(userKeyFile.toPath())
                return Json.parse(content).getString("key")
            } catch (ex: JsonException) {
                // fix for invalid JSON regression
                userKeyFile.delete()
            }
        }

        val key = "user-${UUID.randomUUID()}"
        val keyObject = Json.createObject()
        keyObject.put("key", key)
        Files.write(userKeyFile.toPath(), keyObject.toJson().toByteArray(Charset.defaultCharset()))
        return key
    }

    /**
     * Gets the hotswap-agent.jar location in ~/.vaadin.
     *
     * @return the hotswap-agent.jar file
     */
    fun getHotSwapAgentJar(): File {
        // might only happen if user removes hotswap-agent.jar manually after plugin is already
        // installed
        if (!hotSwapAgentJarFile.exists()) {
            updateOrInstallHotSwapJar()
        }
        return hotSwapAgentJarFile
    }

    /**
     * Installs or updates hotswap-agent.jar in ~/.vaadin
     *
     * @return version of installed hotswap-agent.jar or null in case of error
     */
    fun updateOrInstallHotSwapJar(): String? {
        try {
            if (!hotSwapAgentJarFile.exists()) {
                try {
                    check(FileUtil.createParentDirs(hotSwapAgentJarFile)) {
                        "Unable to create directory for hotswap-agent.jar"
                    }
                    copyBundledHotswapAgentJar()
                    val version = getHotswapAgentVersion(hotSwapAgentJarFile)
                    LOG.info("Installed hotswap-agent.jar version: $version")
                    return version
                } catch (e: IOException) {
                    throw IllegalStateException(
                        "Unable to copy hotswap-agent.jar to " + hotSwapAgentJarFile.absolutePath,
                        e,
                    )
                }
            } else if (isBundledVersionNewer()) {
                try {
                    copyBundledHotswapAgentJar()
                    val version = getHotswapAgentVersion(hotSwapAgentJarFile)
                    LOG.info("Updated hotswap-agent.jar to version $version")
                    return version
                } catch (e: IOException) {
                    throw IllegalStateException("Unable to update hotswap-agent.jar", e)
                }
            } else {
                val version = getHotswapAgentVersion(hotSwapAgentJarFile)
                LOG.info("Using existing hotswap-agent.jar version $version")
                return version
            }
        } catch (e: Exception) {
            LOG.error(e.message, e)
            return null
        }
    }

    private fun isBundledVersionNewer(): Boolean {
        val hotswapAgentVersionInVaadinFolder = getHotswapAgentVersion(hotSwapAgentJarFile)
        val bundledHotswapAgentVersion = getBundledHotswapAgentVersion()
        if (bundledHotswapAgentVersion != null && hotswapAgentVersionInVaadinFolder != null) {
            return VersionComparatorUtil.compare(bundledHotswapAgentVersion, hotswapAgentVersionInVaadinFolder) > 0
        }
        return false
    }

    private fun copyBundledHotswapAgentJar() {
        openBundledHotswapAgentJar().use {
            Files.copy(it, hotSwapAgentJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun openBundledHotswapAgentJar(): InputStream =
        this.javaClass.classLoader.getResourceAsStream(HOTSWAP_AGENT_JAR_FILE_NAME)
            ?: throw IllegalStateException("The plugin package is broken: no hotswap-agent.jar found")

    @VisibleForTesting
    internal fun getBundledHotswapAgentVersion(): String? {
        try {
            return openBundledHotswapAgentJar().use { readHotswapAgentVersion(it) }
        } catch (e: IOException) {
            LOG.error(e.message, e)
            return null
        }
    }

    private fun getHotswapAgentVersion(file: File): String? = file.inputStream().use { readHotswapAgentVersion(it) }

    private fun readHotswapAgentVersion(jar: InputStream): String? {
        val jarStream = JarInputStream(jar)
        var entry = jarStream.nextJarEntry
        while (entry != null) {
            if (entry.name == "version.properties") {
                val properties = Properties()
                properties.load(jarStream)
                return properties.getProperty("version")
            }
            entry = jarStream.nextJarEntry
        }
        return null
    }

    private val intellijFolder: File
        get() = File(resolveVaadinHomeDirectory(), "intellij-plugin")

    private val hotSwapAgentJarFile: File
        get() = File(intellijFolder, HOTSWAP_AGENT_JAR_FILE_NAME)
}
