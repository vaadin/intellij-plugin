package com.vaadin.plugin.starter

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project

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
        return "https://start.vaadin.com/helloworld?framework=$framework&language=$language&buildtool=$buildTool&stack=$architecture"
    }

    override fun getProjectType(): String {
        return buildTool
    }
}
