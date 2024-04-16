package com.vaadin.plugin.starter

class StarterModel(
    var framework: String,      // vaadin version / hilla-lit / hilla-react
    var language: String,       // 17 / kotlin
    var buildTool: String,      // maven / gradle
    var architecture: String    // springboot / servlet / ...
) {

    fun downloadLink(): String {
        var key: String
        if (framework.contains("hilla")) {
            key = framework
            if (buildTool == "gradle") {
                key += "-gradle"
            }
            return StarterSupport.downloadLinks[key]!!
        }

        if (language == "kotlin") {
            key = "kotlin"
        } else if (buildTool == "gradle") {
            key = "gradle-$architecture"
        } else {
            key = architecture
        }

        val link = StarterSupport.downloadLinks[key] ?: "#"
        return link.replace("<version>", framework)
    }

}
