package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class WriteFileHandlerTest {

    @Test
    fun testWriteFileHandlerWithFileOutsideProject() {
        // Given
        val mockProject = mock(Project::class.java)
        val outsideFile = "/tmp/outside_project_file.txt"
        val content = "Test content"
        val data = mapOf(
            "file" to outsideFile,
            "content" to content,
            "undoLabel" to "Test Write"
        )
        val handler = WriteFileHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status)
    }

    @Test
    fun testWriteFileHandlerDataExtraction() {
        // Given
        val mockProject = mock(Project::class.java)
        val testFilePath = "/path/to/test/file.txt"
        val content = "Test file content"
        val undoLabel = "Custom Undo Label"
        val data = mapOf(
            "file" to testFilePath,
            "content" to content,
            "undoLabel" to undoLabel
        )
        
        // When
        val handler = WriteFileHandler(mockProject, data)
        
        // Then - constructor should complete without errors
        assertNotNull(handler)
    }

    @Test
    fun testWriteFileHandlerWithMissingUndoLabel() {
        // Given
        val mockProject = mock(Project::class.java)
        val testFilePath = "/path/to/test/file.txt"
        val content = "Test file content"
        val data = mapOf(
            "file" to testFilePath,
            "content" to content
            // undoLabel is missing - should use default
        )
        
        // When
        val handler = WriteFileHandler(mockProject, data)
        
        // Then - constructor should complete without errors
        assertNotNull(handler)
    }

    @Test
    fun testWriteFileHandlerConstructorWithValidData() {
        // Given
        val mockProject = mock(Project::class.java)
        val content = "Test content with\nmultiple lines"
        val data = mapOf(
            "file" to "/test/file.txt",
            "content" to content
        )
        
        // When
        val handler = WriteFileHandler(mockProject, data)
        
        // Then
        assertNotNull(handler)
    }

    @Test
    fun testWriteFileHandlerResponseStructure() {
        // Given
        val mockProject = mock(Project::class.java)
        val data = mapOf(
            "file" to "/outside/project/file.txt",
            "content" to "test"
        )
        val handler = WriteFileHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertNotNull(response)
        assertNotNull(response.status)
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status)
    }
}