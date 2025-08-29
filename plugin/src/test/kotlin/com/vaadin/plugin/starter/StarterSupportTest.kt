package com.vaadin.plugin.starter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StarterSupportTest {

    @Test
    fun testFrameworksMapContainsExpectedValues() {
        // Given
        val frameworks = StarterSupport.frameworks

        // When & Then
        assertTrue(frameworks.containsKey("flow"))
        assertTrue(frameworks.containsKey("hilla"))
        assertEquals("Flow / Java", frameworks["flow"])
        assertEquals("Hilla / React", frameworks["hilla"])
    }

    @Test
    fun testLanguagesMapContainsExpectedValues() {
        // Given
        val languages = StarterSupport.languages

        // When & Then
        assertTrue(languages.containsKey("java"))
        assertTrue(languages.containsKey("kotlin"))
        assertEquals("Java", languages["java"])
        assertEquals("Kotlin", languages["kotlin"])
    }

    @Test
    fun testArchitecturesMapContainsExpectedValues() {
        // Given
        val architectures = StarterSupport.architectures

        // When & Then
        assertTrue(architectures.containsKey("springboot"))
        assertTrue(architectures.containsKey("quarkus"))
        assertTrue(architectures.containsKey("jakartaee"))
        assertTrue(architectures.containsKey("servlet"))
        assertEquals("Spring Boot", architectures["springboot"])
        assertEquals("Quarkus", architectures["quarkus"])
    }

    @Test
    fun testBuildToolsMapContainsExpectedValues() {
        // Given
        val buildTools = StarterSupport.buildTools

        // When & Then
        assertTrue(buildTools.containsKey("maven"))
        assertTrue(buildTools.containsKey("gradle"))
        assertEquals("Maven", buildTools["maven"])
        assertEquals("Gradle", buildTools["gradle"])
    }

    @Test
    fun testIsSupportedFrameworkWithFlowAndSpringBoot() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("flow")
        model.architectureProperty.set("springboot")

        // When
        val isSupported = StarterSupport.isSupportedFramework(model, "flow")

        // Then
        assertTrue(isSupported)
    }

    @Test
    fun testIsSupportedFrameworkWithHillaAndQuarkus() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("hilla")
        model.architectureProperty.set("quarkus")

        // When
        val isSupported = StarterSupport.isSupportedFramework(model, "hilla")

        // Then
        assertFalse(isSupported) // Hilla only supports springboot
    }

    @Test
    fun testIsSupportedLanguageWithFlowAndJava() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("flow")

        // When
        val isSupported = StarterSupport.isSupportedLanguage(model, "java")

        // Then
        assertTrue(isSupported)
    }

    @Test
    fun testIsSupportedLanguageWithHillaAndKotlin() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("hilla")

        // When
        val isSupported = StarterSupport.isSupportedLanguage(model, "kotlin")

        // Then
        assertFalse(isSupported) // Hilla only supports java
    }

    @Test
    fun testIsSupportedArchitectureWithFlowAndSpringBoot() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("flow")

        // When
        val isSupported = StarterSupport.isSupportedArchitecture(model, "springboot")

        // Then
        assertTrue(isSupported)
    }

    @Test
    fun testIsSupportedBuildToolWithGradleAndSpringBoot() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("flow")
        model.architectureProperty.set("springboot")
        model.buildToolProperty.set("gradle")

        // When
        val isSupported = StarterSupport.isSupportedBuildTool(model, "gradle")

        // Then
        assertTrue(isSupported)
    }

    @Test
    fun testIsSupportedBuildToolWithKotlinAndGradle() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("flow")
        model.languageProperty.set("kotlin")

        // When
        val isSupported = StarterSupport.isSupportedBuildTool(model, "gradle")

        // Then
        assertFalse(isSupported) // Kotlin only supports maven
    }

    @Test
    fun testSupportsAllArchitecturesWithFlow() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("flow")

        // When
        val supportsAll = StarterSupport.supportsAllArchitectures(model)

        // Then
        assertTrue(supportsAll)
    }

    @Test
    fun testSupportsAllArchitecturesWithHilla() {
        // Given
        val model = HelloWorldModel()
        model.frameworkProperty.set("hilla")

        // When
        val supportsAll = StarterSupport.supportsAllArchitectures(model)

        // Then
        assertFalse(supportsAll) // Hilla doesn't support all architectures
    }
}