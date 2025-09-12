package com.vaadin.plugin.starter

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HelloWorldModelTest {
    @Test
    fun defaultGroupIdIsComVaadinApplication() {
        val groupIdProperty = PropertyGraph().property("com.vaadin.application")
        val model = HelloWorldModel(groupIdProperty)
        assertEquals("com.vaadin.application", model.groupIdProperty.get())
    }

    @Test
    fun groupIdCanBeChanged() {
        val groupIdProperty = PropertyGraph().property("com.vaadin.application")
        val model = HelloWorldModel(groupIdProperty)
        groupIdProperty.set("org.example")
        assertEquals("org.example", model.groupIdProperty.get())
    }

    @Test
    fun groupIdIsIncludedInDownloadUrl() {
        val groupIdProperty = PropertyGraph().property("com.vaadin.application")
        val model = HelloWorldModel(groupIdProperty)
        groupIdProperty.set("org.test")
        val project = mock<Project>()
        whenever(project.name).thenReturn("my-app")
        val url = model.getDownloadLink(project)
        assertTrue(url.contains("groupId=org.test"))
    }
}
