package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class GetModulePathsHandlerTest {

    @Test
    fun testGetModulePathsHandlerConstructor() {
        // Given
        val mockProject = mock(Project::class.java)

        // When
        val handler = GetModulePathsHandler(mockProject)

        // Then - constructor should complete without errors
        assertNotNull(handler)
        assertEquals(mockProject, handler.project)
    }

    @Test
    fun testGetModulePathsHandlerInheritsFromAbstractHandler() {
        // Given
        val mockProject = mock(Project::class.java)
        val handler = GetModulePathsHandler(mockProject)

        // Then - should inherit from AbstractHandler
        assertTrue(handler is AbstractHandler)
        assertEquals(mockProject, handler.project)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertTrue(condition)
    }
}
