package com.vaadin.plugin.starter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.intellij.openapi.project.Project
import com.intellij.openapi.observable.properties.PropertyGraph

class StarterProjectModelTest {
    @Test
    fun defaultGroupIdIsComVaadinApplication() {
        val groupIdProperty = PropertyGraph().property("com.vaadin.application")
        val model = StarterProjectModel(groupIdProperty)
        assertEquals("com.vaadin.application", model.groupIdProperty.get())
    }

    @Test
    fun groupIdCanBeChanged() {
        val groupIdProperty = PropertyGraph().property("com.vaadin.application")
        val model = StarterProjectModel(groupIdProperty)
        groupIdProperty.set("org.example")
        assertEquals("org.example", model.groupIdProperty.get())
    }

    @Test
    fun groupIdIsIncludedInDownloadUrl() {
        val groupIdProperty = PropertyGraph().property("com.vaadin.application")
        val model = StarterProjectModel(groupIdProperty)
        groupIdProperty.set("org.test")
        val project = mock<Project>()
        whenever(project.name).thenReturn("demo")
        val url = model.getDownloadLink(project)
        assertTrue(url.contains("groupId=org.test"))
    }
}
