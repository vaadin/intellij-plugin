package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class RefreshHandlerTest {

    @Test
    fun testRefreshHandlerReturnsOkStatus() {
        // Given
        val mockProject = mock(Project::class.java)
        val refreshHandler = RefreshHandler(mockProject)

        // When
        val response = refreshHandler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertEquals(null, response.data)
    }

    @Test
    fun testRefreshHandlerWithMockedProject() {
        // Given
        val mockProject = mock(Project::class.java)
        val refreshHandler = RefreshHandler(mockProject)

        // When
        val response = refreshHandler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertEquals(null, response.data)
    }
}