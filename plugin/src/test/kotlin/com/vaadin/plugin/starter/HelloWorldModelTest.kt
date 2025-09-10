package com.vaadin.plugin.starter

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

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
        val url = model.getDownloadLink(mock<Project>())
        assertTrue(url.contains("groupId=org.test"))
    }
}
