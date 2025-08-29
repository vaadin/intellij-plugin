package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class DeleteFileHandlerTest {

    @Test
    fun testDeleteFileHandlerWithFileOutsideProject() {
        // Given
        val mockProject = mock(Project::class.java)
        val outsideFile = "/tmp/outside_project_file.txt"
        val data = mapOf("file" to outsideFile)
        val handler = DeleteFileHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status)
    }

    @Test
    fun testDeleteFileHandlerDataExtraction() {
        // Given
        val mockProject = mock(Project::class.java)
        val testFilePath = "/path/to/test/file.txt"
        val data = mapOf("file" to testFilePath)

        // When
        val handler = DeleteFileHandler(mockProject, data)

        // Then - constructor should complete without errors
        assertNotNull(handler)
    }

    @Test
    fun testDeleteFileHandlerResponseStructure() {
        // Given
        val mockProject = mock(Project::class.java)
        val testFilePath = "/nonexistent/file.txt"
        val data = mapOf("file" to testFilePath)
        val handler = DeleteFileHandler(mockProject, data)

        // When
        val response = handler.run()

        // Then
        assertNotNull(response)
        assertNotNull(response.status)
        // Response should be either BAD_REQUEST or INTERNAL_SERVER_ERROR depending on file state
        assertTrue(
            response.status == HttpResponseStatus.BAD_REQUEST ||
                response.status == HttpResponseStatus.INTERNAL_SERVER_ERROR)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertTrue(condition)
    }
}
