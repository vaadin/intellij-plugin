package com.vaadin.plugin.listeners

import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.intellij.util.messages.Topic.ProjectLevel
import java.util.*

interface VaadinProjectListener : EventListener {

    companion object {
        @ProjectLevel
        val TOPIC: Topic<VaadinProjectListener> =
            Topic.create("Vaadin project detected", VaadinProjectListener::class.java)
    }

    fun vaadinProjectDetected(project: Project) {}
}
