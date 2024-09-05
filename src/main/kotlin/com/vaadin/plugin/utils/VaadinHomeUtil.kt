package com.vaadin.plugin.utils

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException

object VaadinHomeUtil {
    private const val PROPERTY_USER_HOME: String = "user.home"
    private const val VAADIN_FOLDER_NAME: String = ".vaadin"

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
        val hotswapAgentJar = File(intellijFolder, "hotswap-agent.jar")
        if (!hotswapAgentJar.exists()) {
            // Try to copy the agent to the JDK
            val bundledHotswap = this.javaClass.classLoader.getResource("hotswap-agent.jar")
                ?: throw IllegalStateException("The plugin package is broken: no hotswap-agent.jar found");
            if (!intellijFolder.exists()) {
                check(intellijFolder.mkdirs()) { "Unable to create directory for hotswap-agent.jar" }
            }
            try {
                IOUtils.copyLarge(bundledHotswap.openStream(), hotswapAgentJar.outputStream())
            } catch (e: IOException) {
                throw IllegalStateException("Unable to copy hotswap-agent.jar to " + hotswapAgentJar.absolutePath, e)
            }
        }
        return hotswapAgentJar;
    }

    private val intellijFolder: File
        get() = File(resolveVaadinHomeDirectory(), "intellij-plugin")
}
