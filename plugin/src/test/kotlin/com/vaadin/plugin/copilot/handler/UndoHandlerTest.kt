package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class UndoHandlerTest {

    @Test
    fun testUndoHandlerWithEmptyFilesList() {
        // Given
        val mockProject = mock(Project::class.java)
        val data = mapOf("files" to emptyList<String>())
        val handler = UndoHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertTrue(response.data!!.containsKey("performed"))
        assertEquals(false, response.data!!["performed"])
    }

    @Test
    fun testUndoHandlerWithNonProjectFiles() {
        // Given
        val mockProject = mock(Project::class.java)
        val outsideFiles = listOf("/tmp/outside1.txt", "/tmp/outside2.txt")
        val data = mapOf("files" to outsideFiles)
        val handler = UndoHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertTrue(response.data!!.containsKey("performed"))
        assertEquals(false, response.data!!["performed"])
    }

    @Test
    fun testUndoHandlerDataExtraction() {
        // Given
        val mockProject = mock(Project::class.java)
        val files = listOf("/path/to/file1.txt", "/path/to/file2.java")
        val data = mapOf("files" to files)

        // When
        val handler = UndoHandler(mockProject, data)

        // Then - constructor should complete without errors
        assertNotNull(handler)
    }

    @Test
    fun testUndoHandlerResponseStructure() {
        // Given
        val mockProject = mock(Project::class.java)
        val data = mapOf("files" to listOf("/outside/project/file.txt"))
        val handler = UndoHandler(mockProject, data)

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
    fun testUndoHandlerWithSingleFile() {
        // Given
        val mockProject = mock(Project::class.java)
        val files = listOf("/test/single/file.txt")
        val data = mapOf("files" to files)
        val handler = UndoHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertEquals(false, response.data!!["performed"]) // Expected since file is not in project
    }
}
