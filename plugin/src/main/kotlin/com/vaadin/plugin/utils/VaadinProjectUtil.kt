package com.vaadin.plugin.utils

import com.intellij.ide.plugins.PluginManager
import com.intellij.java.library.JavaLibraryUtil.hasLibraryClass
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.vaadin.plugin.listeners.VaadinProjectListener
import com.vaadin.plugin.starter.DownloadableModel

const val VAADIN_SERVICE = "com.vaadin.flow.server.VaadinService"

internal const val ENDPOINTS_PLUGIN_ID = "com.intellij.microservices.ui"

class VaadinProjectUtil {

    companion object {

        val PROJECT_MODEL_PROP_KEY = Key<GraphProperty<DownloadableModel?>>("vaadin_project_model")
    }
}

internal fun doNotifyAboutVaadinProject(project: Project) {
    val publisher: VaadinProjectListener = project.messageBus.syncPublisher(VaadinProjectListener.TOPIC)
    publisher.vaadinProjectDetected(project)
}

internal fun hasVaadin(project: Project): Boolean = hasLibraryClass(project, VAADIN_SERVICE)

internal fun hasVaadin(module: com.intellij.openapi.module.Module): Boolean = hasLibraryClass(module, VAADIN_SERVICE)

internal fun hasEndpoints(): Boolean =
    PluginId.findId(ENDPOINTS_PLUGIN_ID)?.let { PluginManager.isPluginInstalled(it) } ?: false

internal fun isUltimate(): Boolean = ApplicationInfo.getInstance().apiVersion.startsWith("IU-")
