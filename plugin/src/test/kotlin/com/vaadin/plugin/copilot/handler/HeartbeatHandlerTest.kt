package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.service.CompilationStatusManagerService
import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class HeartbeatHandlerTest {

    @Test
    fun testHeartbeatHandlerWithNoCompilationErrors() {
        // Given
        val mockProject = mock(Project::class.java)
        val mockCompilationService = mock(CompilationStatusManagerService::class.java)
        
        `when`(mockProject.getService(CompilationStatusManagerService::class.java))
            .thenReturn(mockCompilationService)
        `when`(mockCompilationService.hasCompilationError()).thenReturn(false)
        `when`(mockCompilationService.getErrorFiles()).thenReturn(emptyList())
        
        val heartbeatHandler = HeartbeatHandler(mockProject)

        // When
        val response = heartbeatHandler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertTrue(response.data!!.containsKey(HeartbeatHandler.HAS_COMPILATION_ERROR))
        assertTrue(response.data!!.containsKey(HeartbeatHandler.FILES_CONTAIN_COMPILATION_ERROR))
        assertEquals(false, response.data!![HeartbeatHandler.HAS_COMPILATION_ERROR])
        assertEquals(emptyList<String>(), response.data!![HeartbeatHandler.FILES_CONTAIN_COMPILATION_ERROR])
    }

    @Test
    fun testHeartbeatHandlerWithCompilationErrors() {
        // Given
        val mockProject = mock(Project::class.java)
        val mockCompilationService = mock(CompilationStatusManagerService::class.java)
        val errorFiles = listOf("Test.java", "Example.kt")
        
        `when`(mockProject.getService(CompilationStatusManagerService::class.java))
            .thenReturn(mockCompilationService)
        `when`(mockCompilationService.hasCompilationError()).thenReturn(true)
        `when`(mockCompilationService.getErrorFiles()).thenReturn(errorFiles)
        
        val heartbeatHandler = HeartbeatHandler(mockProject)

        // When
        val response = heartbeatHandler.run()

        // Then
        assertEquals(HttpResponseStatus.OK, response.status)
        assertNotNull(response.data)
        assertEquals(true, response.data!![HeartbeatHandler.HAS_COMPILATION_ERROR])
        assertEquals(errorFiles, response.data!![HeartbeatHandler.FILES_CONTAIN_COMPILATION_ERROR])
    }
}