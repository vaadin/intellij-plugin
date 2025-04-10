package com.vaadin.plugin.starter

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HelloWorldModel : DownloadableModel {

    private val graph = PropertyGraph()
    val frameworkProperty = graph.property(StarterSupport.frameworks.keys.first())
    val languageProperty = graph.property(StarterSupport.languages.keys.first())
    val buildToolProperty = graph.property(StarterSupport.buildTools.keys.first())
    val architectureProperty = graph.property(StarterSupport.architectures.keys.first())

    val framework by frameworkProperty
    val language by languageProperty
    val buildTool by buildToolProperty
    val architecture by architectureProperty

    override fun getDownloadLink(project: Project): String {
        val params =
            mapOf(
                "framework" to framework,
                "language" to language,
                "buildtool" to buildTool,
                "stack" to architecture,
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
