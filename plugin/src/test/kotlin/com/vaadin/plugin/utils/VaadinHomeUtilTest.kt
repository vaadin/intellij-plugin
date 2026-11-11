package com.vaadin.plugin.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class VaadinHomeUtilTest {

    @TempDir lateinit var home: Path

    private var originalUserHome: String? = null

    @BeforeEach
    fun redirectUserHome() {
        originalUserHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
    }

    @AfterEach
    fun restoreUserHome() {
        System.setProperty("user.home", originalUserHome!!)
    }

    @Test
    fun snapshotOfTheBundledVersionIsReplacedByTheRelease() {
        val installed = installFakeAgent(bundledVersion() + "-SNAPSHOT")

        VaadinHomeUtil.updateOrInstallHotSwapJar()

        assertArrayEquals(bundledJar(), Files.readAllBytes(installed.toPath()))
    }

    @Test
    fun newerInstalledVersionIsKept() {
        val installed = installFakeAgent("10.0.0")
        val before = Files.readAllBytes(installed.toPath())

        VaadinHomeUtil.updateOrInstallHotSwapJar()

        assertArrayEquals(before, Files.readAllBytes(installed.toPath()))
    }

    private fun installFakeAgent(version: String): File {
        val jar = VaadinHomeUtil.getHotSwapAgentJar()
        jar.parentFile.mkdirs()
        JarOutputStream(jar.outputStream()).use {
            it.putNextEntry(JarEntry("version.properties"))
            it.write("version=$version\n".toByteArray())
            it.closeEntry()
        }
        return jar
    }

    private fun bundledJar(): ByteArray =
        javaClass.classLoader.getResourceAsStream("hotswap-agent.jar")!!.use { it.readAllBytes() }

    private fun bundledVersion(): String {
        val copy = Files.write(home.resolve("bundled.jar"), bundledJar())
        return JarFile(copy.toFile()).use { jar ->
            val properties = Properties()
            jar.getInputStream(jar.getEntry("version.properties")).use { properties.load(it) }
            properties.getProperty("version")
        }
    }
}
