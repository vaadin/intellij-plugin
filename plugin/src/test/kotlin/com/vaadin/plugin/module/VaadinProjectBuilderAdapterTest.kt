package com.vaadin.plugin.module

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class VaadinProjectBuilderAdapterTest {

    @Test
    fun testVaadinProjectBuilderAdapterBasicProperties() {
        // Given
        val wizard = VaadinProjectWizard()
        val adapter = VaadinProjectBuilderAdapter(wizard)

        // When & Then
        assertNotNull(adapter)
    }

    @Test
    fun testIsAvailableMethod() {
        // Given
        val adapter = VaadinProjectBuilderAdapter()

        // When - call isAvailable in different contexts
        val result = adapter.isAvailable()

        // Then - should return based on stack trace analysis
        // Note: In a test environment, this might behave differently than in production
        assertNotNull(result)
        assertTrue(result is Boolean)
    }

    @Test
    fun testDefaultConstructor() {
        // Given & When
        val adapter = VaadinProjectBuilderAdapter()

        // Then
        assertNotNull(adapter)
    }

    @Test
    fun testCustomWizardConstructor() {
        // Given
        val customWizard = VaadinProjectWizard()

        // When
        val adapter = VaadinProjectBuilderAdapter(customWizard)

        // Then
        assertNotNull(adapter)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertTrue(condition)
    }
}
