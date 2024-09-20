package com.vaadin.plugin.starter

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import java.net.URLEncoder

class QuickStarterModel : BaseState(), DownloadableModel {

    companion object {
        val VIEWS = listOf("Flow (Java)", "Hilla (React)", "None")
    }

    private val graph: PropertyGraph = PropertyGraph()
    val exampleViewsProperty = graph.property(VIEWS.first())
    val useAuthenticationProperty = graph.property(false)
    val usePrereleaseProperty = graph.property(false)

    private val exampleViews by exampleViewsProperty
    private val useAuthentication by useAuthenticationProperty
    private val usePrerelease by usePrereleaseProperty

    override fun getDownloadLink(project: Project): String {
        var preset =
            if (exampleViews == VIEWS[0]) {
                "default"
            } else if (exampleViews == VIEWS[1]) {
                "react"
            } else {
                "empty"
            }

        if (useAuthentication) {
            preset += "&preset=partial-auth"
        }
        if (usePrerelease) {
            preset += "&preset=partial-prerelease"
        }

        return "https://start.vaadin.com/dl?preset=${preset}&projectName=${
            URLEncoder.encode(
                project.name,
                "UTF-8",
            ).replace("\\+", "%20")
        }"
    }

    override fun getProjectType(): String {
        return "maven"
    }
}
