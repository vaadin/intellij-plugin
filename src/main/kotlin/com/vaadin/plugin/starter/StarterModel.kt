package com.vaadin.plugin.starter

import com.intellij.openapi.project.Project

class StarterModel(
    var framework: String,      // flow / hilla-react
    var language: String,       // java / kotlin
    var buildTool: String,      // maven / gradle
    var architecture: String    // springboot / servlet / ...
) : DownloadableModel {

    override fun getDownloadLink(project: Project): String {
        return "https://start.vaadin.com/helloworld?framework=$framework&language=$language&buildtool=$buildTool&stack=$architecture"
    }

    override fun getProjectType(): String {
        return buildTool
    }

}
