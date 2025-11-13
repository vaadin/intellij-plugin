package com.vaadin.plugin.starter

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.vaadin.plugin.utils.toArtifactId
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HelloWorldModel(val groupIdProperty: com.intellij.openapi.observable.properties.GraphProperty<String>) :
    DownloadableModel {

    private val graph = PropertyGraph()
    val languageProperty = graph.property(StarterSupport.languages.keys.first())
    val buildToolProperty = graph.property(StarterSupport.buildTools.keys.first())
    val architectureProperty = graph.property(StarterSupport.architectures.keys.first())

    val language by languageProperty
    val buildTool by buildToolProperty
    val architecture by architectureProperty
    val groupId by groupIdProperty

    override fun getDownloadLink(project: Project): String {
        val params =
            mapOf(
                "framework" to "flow",
                "language" to language,
                "buildtool" to buildTool,
                "stack" to architecture,
                "artifactId" to toArtifactId(project.name),
                "groupId" to groupId,
                "ref" to "intellij-plugin")
        val query =
            params.entries.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
            }

        return "https://start.vaadin.com/helloworld?${query}"
    }

    override fun getProjectType(): String {
        return buildTool
    }
}
