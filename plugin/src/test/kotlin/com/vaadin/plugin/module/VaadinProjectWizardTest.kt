package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class VaadinProjectWizardTest {

    @Test
    fun testVaadinProjectWizardBasicProperties() {
        // Given
        val wizard = VaadinProjectWizard()

        // When & Then
        assertEquals("Vaadin", wizard.id)
        assertEquals("Vaadin", wizard.name)
        assertNotNull(wizard.icon)
    }

    @Test
    fun testVaadinProjectWizardCreateStep() {
        // Given
        val wizard = VaadinProjectWizard()
        val context = mock(WizardContext::class.java)

        // When
        val step = wizard.createStep(context)

        // Then
        assertNotNull(step)
        assertEquals(context, step.context)
        assertNotNull(step.propertyGraph)
        assertNotNull(step.data)
        assertNotNull(step.keywords)
    }

    @Test
    fun testVaadinProjectWizardStepProperties() {
        // Given
        val context = mock(WizardContext::class.java)
        val propertyGraph = PropertyGraph("Test")
        val step = VaadinProjectWizardStep(context, propertyGraph)

        // When & Then
        assertEquals(context, step.context)
        assertEquals(propertyGraph, step.propertyGraph)
        assertNotNull(step.data)
        assertNotNull(step.keywords)
    }

    @Test
    fun testVaadinProjectWizardPropertyGraph() {
        // Given
        val wizard = VaadinProjectWizard()

        // When & Then
        assertNotNull(wizard.projectModel) // Initially null
    }
}