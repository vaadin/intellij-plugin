package com.vaadin.plugin.starter

import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class HelloWorldModelTest {

    @Test
    fun testHelloWorldModelDefaultValues() {
        // Given & When
        val model = HelloWorldModel()

        // Then
        assertEquals("flow", model.framework)
        assertEquals("java", model.language)
        assertEquals("maven", model.buildTool)
        assertEquals("springboot", model.architecture)
    }

    @Test
    fun testHelloWorldModelPropertyModification() {
        // Given
        val model = HelloWorldModel()

        // When
        model.frameworkProperty.set("hilla")
        model.languageProperty.set("kotlin")
        model.buildToolProperty.set("gradle")
        model.architectureProperty.set("quarkus")

        // Then
        assertEquals("hilla", model.framework)
        assertEquals("kotlin", model.language)
        assertEquals("gradle", model.buildTool)
        assertEquals("quarkus", model.architecture)
    }

    @Test
    fun testGetDownloadLinkWithDefaultValues() {
        // Given
        val model = HelloWorldModel()
        val mockProject = mock(Project::class.java)

        // When
        val downloadLink = model.getDownloadLink(mockProject)

        // Then
        assertNotNull(downloadLink)
        assertTrue(downloadLink.startsWith("https://start.vaadin.com/helloworld?"))
        assertTrue(downloadLink.contains("framework=flow"))
        assertTrue(downloadLink.contains("language=java"))
        assertTrue(downloadLink.contains("buildtool=maven"))
        assertTrue(downloadLink.contains("stack=springboot"))
        assertTrue(downloadLink.contains("ref=intellij-plugin"))
    }

    @Test
    fun testGetDownloadLinkWithCustomValues() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("hilla")
        model.languageProperty.set("java")
        model.buildToolProperty.set("gradle")
        model.architectureProperty.set("springboot")
        val mockProject = mock(Project::class.java)

        // When
        val downloadLink = model.getDownloadLink(mockProject)

        // Then
        assertNotNull(downloadLink)
        assertTrue(downloadLink.startsWith("https://start.vaadin.com/helloworld?"))
        assertTrue(downloadLink.contains("framework=hilla"))
        assertTrue(downloadLink.contains("language=java"))
        assertTrue(downloadLink.contains("buildtool=gradle"))
        assertTrue(downloadLink.contains("stack=springboot"))
        assertTrue(downloadLink.contains("ref=intellij-plugin"))
    }

    @Test
    fun testGetProjectTypeReturnsBuilTool() {
        // Given
        val model = HelloWorldModel()

        // When
        val projectType = model.getProjectType()

        // Then
        assertEquals(model.buildTool, projectType)
        assertEquals("maven", projectType)
    }

    @Test
    fun testGetProjectTypeWithGradle() {
        // Given
        val model = HelloWorldModel()
        model.buildToolProperty.set("gradle")

        // When
        val projectType = model.getProjectType()

        // Then
        assertEquals("gradle", projectType)
    }

    @Test
    fun testDownloadLinkURLEncoding() {
        // Given
        val model = HelloWorldModel()
        val mockProject = mock(Project::class.java)

        // When
        val downloadLink = model.getDownloadLink(mockProject)

        // Then
        // Verify URL encoding is applied (spaces should be encoded, special chars handled)
        assertNotNull(downloadLink)
        assertTrue(downloadLink.contains("&"))
        assertTrue(downloadLink.contains("="))
        // The URL should not contain spaces (they should be encoded)
        assertFalse(downloadLink.contains(" "))
    }

    private fun assertFalse(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertFalse(condition)
    }
}