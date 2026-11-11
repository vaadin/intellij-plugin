package com.vaadin.plugin.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class VaadinHomeUtilTest {

    @TempDir lateinit var home: Path

    @BeforeEach
    fun redirectVaadinHome() {
        VaadinHomeUtil.vaadinHomeDirectoryOverride = home.toFile()
    }

    @AfterEach
    fun restoreVaadinHome() {
        VaadinHomeUtil.vaadinHomeDirectoryOverride = null
    }

    @Test
    fun snapshotOfTheBundledVersionIsReplacedByTheRelease() {
        assertReplaced(bundledVersion() + "-SNAPSHOT")
    }

    @Test
    fun olderSnapshotIsReplaced() {
        assertReplaced("2.0.4-SNAPSHOT")
    }

    @Test
    fun sameVersionIsKept() {
        assertKept(bundledVersion())
    }

    @Test
    fun newerInstalledVersionIsKept() {
        assertKept("10.0.0")
    }

    private fun assertReplaced(installedVersion: String) {
        val installed = installFakeAgent(installedVersion)

        VaadinHomeUtil.updateOrInstallHotSwapJar()

        assertArrayEquals(bundledJar(), Files.readAllBytes(installed.toPath()))
    }

    private fun assertKept(installedVersion: String) {
        val installed = installFakeAgent(installedVersion)
        val before = Files.readAllBytes(installed.toPath())

        VaadinHomeUtil.updateOrInstallHotSwapJar()

        assertArrayEquals(before, Files.readAllBytes(installed.toPath()))
    }

    private fun installFakeAgent(version: String): File {
        val jar = File(home.toFile(), "intellij-plugin/hotswap-agent.jar")
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

    private fun bundledVersion(): String = VaadinHomeUtil.getBundledHotswapAgentVersion()!!
}
