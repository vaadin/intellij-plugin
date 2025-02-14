package com.vaadin.plugin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import elemental.json.Json
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.*
import java.util.jar.JarFile

object VaadinHomeUtil {
    private val LOG: Logger = Logger.getInstance(VaadinHomeUtil::class.java)
    private const val PROPERTY_USER_HOME: String = "user.home"
    private const val VAADIN_FOLDER_NAME: String = ".vaadin"
    private const val HOTSWAP_AGENT_JAR_FILE_NAME = "hotswap-agent.jar"

    /**
     * Get Vaadin home directory.
     *
     * @return File instance for Vaadin home folder. Does not check if the folder exists.
     */
    private fun resolveVaadinHomeDirectory(): File {
        val userHome = System.getProperty(PROPERTY_USER_HOME)
        return File(userHome, VAADIN_FOLDER_NAME)
    }

    @Throws(IOException::class)
    fun getUserKey(): String {
        val vaadinHome = resolveVaadinHomeDirectory()
        val userKeyFile = File(vaadinHome, "userKey")
        if (userKeyFile.exists()) {
            val content = Files.readString(userKeyFile.toPath())
            return Json.parse(content).getString("key")
        } else {
            val key = "user-${UUID.randomUUID()}"
            val keyObject = Json.createObject()
            keyObject.put("key", key)
            Files.write(userKeyFile.toPath(), keyObject.toJson().toByteArray(Charset.defaultCharset()))
            return key
        }
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
            val bundledHotswap =
                this.javaClass.classLoader.getResource(HOTSWAP_AGENT_JAR_FILE_NAME)
                    ?: throw IllegalStateException("The plugin package is broken: no hotswap-agent.jar found")
            if (!hotSwapAgentJarFile.exists()) {
                try {
                    check(FileUtil.createParentDirs(hotSwapAgentJarFile)) {
                        "Unable to create directory for hotswap-agent.jar"
                    }
                    FileUtil.copy(bundledHotswap.openStream(), hotSwapAgentJarFile.outputStream())
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
                    FileUtil.copy(bundledHotswap.openStream(), hotSwapAgentJarFile.outputStream())
                    val version = getHotswapAgentVersion(hotSwapAgentJarFile)
                    LOG.info("Updated hotswap-agent.jar to version $version")
                    return version
                } catch (e: IOException) {
                    throw IllegalStateException("Unable to update hotswap-agent.jar", e)
                }
            } else {
                val version = getHotswapAgentVersion(hotSwapAgentJarFile)
                LOG.info("Using existing hotswap-agent.jar version " + getHotswapAgentVersion(hotSwapAgentJarFile))
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
            return bundledHotswapAgentVersion.compareTo(hotswapAgentVersionInVaadinFolder) == 1
        }
        return false
    }

    private fun getBundledHotswapAgentVersion(): String? {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("bundled-hotswap-agent", ".jar")
            FileUtil.copy(
                this.javaClass.classLoader.getResource(HOTSWAP_AGENT_JAR_FILE_NAME)?.openStream()
                    ?: throw IllegalStateException("Unable to copy hotswap-agent.jar to temporary file "),
                tempFile.outputStream(),
            )
            return getHotswapAgentVersion(tempFile)
        } catch (e: IOException) {
            LOG.error(e.message, e)
            return null
        } finally {
            if (tempFile != null) {
                FileUtil.asyncDelete(tempFile)
            }
        }
    }

    private fun getHotswapAgentVersion(file: File): String? {
        val jarFile = JarFile(file)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name == "version.properties") {
                val inputStream = jarFile.getInputStream(entry)
                val properties = Properties()
                properties.load(inputStream)
                var version = properties.getProperty("version")
                if (version.indexOf('-') != -1) {
                    version = version.substring(0, version.indexOf('-'))
                }
                return version
            }
        }
        return null
    }

    private val intellijFolder: File
        get() = File(resolveVaadinHomeDirectory(), "intellij-plugin")

    private val hotSwapAgentJarFile: File
        get() = File(intellijFolder, HOTSWAP_AGENT_JAR_FILE_NAME)
}
