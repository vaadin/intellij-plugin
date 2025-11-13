package com.vaadin.plugin.starter

class StarterSupport {

    companion object {

        val languages = linkedMapOf("java" to "Java", "kotlin" to "Kotlin")

        val architectures =
            linkedMapOf(
                "springboot" to "Spring Boot",
                "quarkus" to "Quarkus",
                "jakartaee" to "Jakarta EE",
                "servlet" to "Servlet",
            )

        val buildTools = linkedMapOf("maven" to "Maven", "gradle" to "Gradle")

        fun isSupportedArchitecture(model: HelloWorldModel, architecture: String): Boolean {
            if (model.buildTool == "gradle") {
                return setOf("springboot", "servlet").contains(architecture)
            } else if (model.language == "kotlin") {
                return architecture == "springboot"
            }
            return true
        }

        fun isSupportedBuildTool(model: HelloWorldModel, buildTool: String): Boolean {
            if (model.language == "kotlin") {
                return buildTool == "maven"
            }
            return true
        }

    }
}
