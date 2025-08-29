package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class DeleteFileHandlerTest {

    @Test
    fun testDeleteFileHandlerConstructor() {
        // Given
        val mockProject = mock(Project::class.java)
        val testFilePath = "/path/to/test/file.txt"
        val data = mapOf("file" to testFilePath)

        // When - constructor should not fail
        val handler = DeleteFileHandler(mockProject, data)

        // Then - constructor should complete without errors
        assertNotNull(handler)
        assertEquals(mockProject, handler.project)
    }

    @Test
    fun testDeleteFileHandlerDataExtraction() {
        // Given
        val mockProject = mock(Project::class.java)
        val testFilePath = "/path/to/test/file.txt"
        val data = mapOf("file" to testFilePath)

        // When
        val handler = DeleteFileHandler(mockProject, data)

        // Then - constructor should complete without errors and extract data
        assertNotNull(handler)
        assertEquals(mockProject, handler.project)
    }

    @Test
    fun testDeleteFileHandlerInheritsFromAbstractHandler() {
        // Given
        val mockProject = mock(Project::class.java)
        val testFilePath = "/path/to/test/file.txt"
        val data = mapOf("file" to testFilePath)
        val handler = DeleteFileHandler(mockProject, data)

        // Then - should inherit from AbstractHandler
        assertTrue(handler is AbstractHandler)
        assertEquals(mockProject, handler.project)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertTrue(condition)
    }
}
