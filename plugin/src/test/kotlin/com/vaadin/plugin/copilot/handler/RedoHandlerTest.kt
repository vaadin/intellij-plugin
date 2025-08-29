package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class RedoHandlerTest {

    @Test
    fun testRedoHandlerWithEmptyFilesList() {
        // Given
        val mockProject = mock(Project::class.java)
        val data = mapOf("files" to emptyList<String>())
        val handler = RedoHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertTrue(response.data!!.containsKey("performed"))
        assertEquals(false, response.data!!["performed"])
    }

    @Test
    fun testRedoHandlerWithNonProjectFiles() {
        // Given
        val mockProject = mock(Project::class.java)
        val outsideFiles = listOf("/tmp/outside1.txt", "/tmp/outside2.txt")
        val data = mapOf("files" to outsideFiles)
        val handler = RedoHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertTrue(response.data!!.containsKey("performed"))
        assertEquals(false, response.data!!["performed"])
    }

    @Test
    fun testRedoHandlerDataExtraction() {
        // Given
        val mockProject = mock(Project::class.java)
        val files = listOf("/path/to/file1.txt", "/path/to/file2.java")
        val data = mapOf("files" to files)
        
        // When
        val handler = RedoHandler(mockProject, data)
        
        // Then - constructor should complete without errors
        assertNotNull(handler)
    }

    @Test
    fun testRedoHandlerResponseStructure() {
        // Given
        val mockProject = mock(Project::class.java)
        val data = mapOf("files" to listOf("/outside/project/file.txt"))
        val handler = RedoHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertNotNull(response)
        assertNotNull(response.status)
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertTrue(response.data!!.containsKey("performed"))
    }

    @Test
    fun testRedoHandlerInheritsFromUndoHandler() {
        // Given
        val mockProject = mock(Project::class.java)
        val data = mapOf("files" to emptyList<String>())
        val redoHandler = RedoHandler(mockProject, data)
        val undoHandler = UndoHandler(mockProject, data)

        // When & Then
        // RedoHandler should inherit from UndoHandler
        assertTrue(redoHandler is UndoHandler)
        assertNotNull(redoHandler)
        assertNotNull(undoHandler)
    }

    @Test
    fun testRedoHandlerWithSingleFile() {
        // Given
        val mockProject = mock(Project::class.java)
        val files = listOf("/test/single/file.txt")
        val data = mapOf("files" to files)
        val handler = RedoHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertEquals(false, response.data!!["performed"]) // Expected since file is not in project
    }
}