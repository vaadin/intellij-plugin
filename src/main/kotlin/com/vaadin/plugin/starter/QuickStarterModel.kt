package com.vaadin.plugin.starter

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class QuickStarterModel(
    var views: String,
    var authentication: Boolean,
    var version: String
) : BaseState(), DownloadableModel {

    override fun getDownloadLink(project: Project): String {
        var preset = if (views.contains("Flow")) {
            "default"
        } else if (views.contains("Hilla")) {
            "react"
        } else {
            "empty"
        }

        if (authentication) {
            preset += "&preset=partial-auth"
        }
        if (version === "Prerelease") {
            preset += "&preset=partial-prerelease"
        }

        return "https://start.vaadin.com/dl?preset=${preset}&projectName=${project.name}"
    }

    override fun getProjectType(): String {
        return "maven"
    }
}