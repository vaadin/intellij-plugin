package com.vaadin.plugin.starter

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class QuickStarterModel(
    var framework: String,
    var frontend: String,
    var exampleViews: Boolean,
    var authentication: Boolean,
    var version: String
) : BaseState(), DownloadableModel {

    override fun getDownloadLink(project: Project): String {
        var preset = ""
        if (framework === "Flow") {
            preset = if (exampleViews) "default" else "empty"
        } else if (framework === "Hilla") {
            if (frontend == "Lit") {
                preset = if (exampleViews) "hilla" else "hilla-empty"
            } else if (frontend === "React") {
                preset = if (exampleViews) "react" else "react-empty"
            }
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