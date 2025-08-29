package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.vaadin.plugin.copilot.CopilotPluginUtil
import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`

class GetModulePathsHandlerTest {

    @Test
    fun testGetModulePathsHandlerWithMockedProject() {
        // Given
        val mockProject = mock(Project::class.java)
        val mockVirtualFile = mock(VirtualFile::class.java)
        val expectedPath = "/test/project/path"
        val expectedModules = listOf(
            CopilotPluginUtil.ModuleInfo(
                "test-module",
                listOf("/content/root"),
                arrayListOf("/src/main/java"),
                arrayListOf("/src/test/java"),
                arrayListOf("/src/main/resources"),
                arrayListOf("/src/test/resources"),
                "/target/classes"
            )
        )

        `when`(mockVirtualFile.path).thenReturn(expectedPath)

        mockStatic(CopilotPluginUtil::class.java).use { mockedStatic: MockedStatic<CopilotPluginUtil> ->
            mockedStatic.`when`<List<CopilotPluginUtil.ModuleInfo>> { 
                CopilotPluginUtil.getModulesInfo(mockProject) 
            }.thenReturn(expectedModules)

            val handler = GetModulePathsHandler(mockProject)

            // When
            val response = handler.run()

            // Then
            assertEquals(HttpResponseStatus.OK, response.status)
            assertNotNull(response.data)
            assertTrue(response.data!!.containsKey("project"))
            
            val projectInfo = response.data!!["project"] as CopilotPluginUtil.ProjectInfo
            assertEquals(expectedModules, projectInfo.modules)
        }
    }

    @Test
    fun testGetModulePathsHandlerBasicStructure() {
        // Given
        val mockProject = mock(Project::class.java)
        val expectedModules = emptyList<CopilotPluginUtil.ModuleInfo>()

        mockStatic(CopilotPluginUtil::class.java).use { mockedStatic: MockedStatic<CopilotPluginUtil> ->
            mockedStatic.`when`<List<CopilotPluginUtil.ModuleInfo>> { 
                CopilotPluginUtil.getModulesInfo(mockProject) 
            }.thenReturn(expectedModules)

            val handler = GetModulePathsHandler(mockProject)

            // When
            val response = handler.run()

            // Then
            assertEquals(HttpResponseStatus.OK, response.status)
            assertNotNull(response.data)
            assertTrue(response.data!!.containsKey("project"))
        }
    }
}