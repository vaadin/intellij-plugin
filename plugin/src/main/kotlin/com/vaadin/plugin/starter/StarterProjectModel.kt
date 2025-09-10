package com.vaadin.plugin.starter

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class StarterProjectModel(
    val groupIdProperty: com.intellij.openapi.observable.properties.GraphProperty<String>
) : BaseState(), DownloadableModel {

    private val graph: PropertyGraph = PropertyGraph()
    val usePrereleaseProperty = graph.property(false)
    val includeFlowProperty = graph.property(true)
    val includeHillaProperty = graph.property(false)

    private val usePrerelease by usePrereleaseProperty
    private val includeFlow by includeFlowProperty
    private val includeHilla by includeHillaProperty
    private val groupId by groupIdProperty

    override fun getDownloadLink(project: Project): String {
        val frameworks =
            when {
                includeFlow && includeHilla -> "flow,hilla"
                includeFlow -> "flow"
                includeHilla -> "hilla"
                else -> "empty"
            }

        val platformVersion = if (usePrerelease) "pre" else "latest"

        val params =
            mapOf(
                "frameworks" to frameworks,
                "platformVersion" to platformVersion,
                "artifactId" to toArtifactId(project.name),
                "groupId" to groupId,
                "ref" to "intellij-plugin")
        val query =
            params.entries.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
            }

        return "https://start.vaadin.com/skeleton?${query}"
    }

    override fun getProjectType(): String {
        return "maven"
    }

    private fun toArtifactId(name: String): String {
        return name
            .trim()
            .replace(Regex("([a-z])([A-Z])"), "$1-$2") // camelCase to kebab-case
            .replace(Regex("[\\s_]+"), "-") // spaces/underscores to hyphen
            .replace(Regex("[^a-zA-Z0-9-]"), "") // remove invalid chars
            .lowercase()
    }
}
