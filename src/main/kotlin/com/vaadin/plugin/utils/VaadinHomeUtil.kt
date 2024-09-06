package com.vaadin.plugin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.io.IOException
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
     * @return File instance for Vaadin home folder. Does not check if the
     * folder exists.
     */
    private fun resolveVaadinHomeDirectory(): File {
        val userHome = System
            .getProperty(PROPERTY_USER_HOME)
        return File(userHome, VAADIN_FOLDER_NAME)
    }

    /** Gets the hotswap-agent.jar location in ~/.vaadin.
     *
     * If the file does not exist, copies the bundled version.
     *
     * @return the hotswap-agent.jar file
     */
    fun getHotswapAgentJar(): File {
        val hotswapAgentJar = File(intellijFolder, HOTSWAP_AGENT_JAR_FILE_NAME)
        if (!hotswapAgentJar.exists()) {
            // Try to copy the agent to the JDK
            val bundledHotswap = this.javaClass.classLoader.getResource(HOTSWAP_AGENT_JAR_FILE_NAME)
                ?: throw IllegalStateException("The plugin package is broken: no hotswap-agent.jar found")
            if (!intellijFolder.exists()) {
                check(intellijFolder.mkdirs()) { "Unable to create directory for hotswap-agent.jar" }
            }
            try {
                FileUtil.copy(bundledHotswap.openStream(), hotswapAgentJar.outputStream())
            } catch (e: IOException) {
                throw IllegalStateException("Unable to copy hotswap-agent.jar to " + hotswapAgentJar.absolutePath, e)
            }
        }
        return hotswapAgentJar
    }

    fun checkBundledAndCurrentVersionAndReplaceIfBundledOneIsNewer() {
        try {
            val hotswapAgentJar = File(intellijFolder, HOTSWAP_AGENT_JAR_FILE_NAME)
            val bundledHotswap = this.javaClass.classLoader.getResource(HOTSWAP_AGENT_JAR_FILE_NAME)
                ?: throw IllegalStateException("The plugin package is broken: no hotswap-agent.jar found")
            if (isBundledVersionNewer()) {
                FileUtil.copy(bundledHotswap.openStream(), hotswapAgentJar.outputStream())
            }
        } catch (e: Exception) {
            LOG.error(e.message, e)
        }
    }

    private fun isBundledVersionNewer(): Boolean {
        val hotswapAgentJar = File(intellijFolder, HOTSWAP_AGENT_JAR_FILE_NAME)
        val hotswapAgentVersionInVaadinFolder = getHotswapAgentVersion(hotswapAgentJar)
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
                tempFile.outputStream()
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
}
