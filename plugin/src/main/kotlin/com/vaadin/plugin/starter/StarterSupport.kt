package com.vaadin.plugin.starter

class StarterSupport {

    companion object {

        val frameworks = linkedMapOf("flow" to "Flow / Java", "hilla" to "Hilla / React")

        val languages = linkedMapOf("java" to "Java", "kotlin" to "Kotlin")

        val architectures =
            linkedMapOf(
                "springboot" to "Spring Boot",
                "quarkus" to "Quarkus",
                "jakartaee" to "Jakarta EE",
                "servlet" to "Servlet",
            )

        val buildTools = linkedMapOf("maven" to "Maven", "gradle" to "Gradle")

        private val supportMatrix =
            arrayOf(
                StarterSupportMatrixElement(
                    "flow",
                    languages.keys,
                    setOf("springboot", "quarkus", "jakartaee", "servlet"),
                    buildTools.keys,
                ),
                StarterSupportMatrixElement("hilla", setOf("java"), setOf("springboot"), buildTools.keys),
            )

        fun isSupportedFramework(model: HelloWorldModel, framework: String): Boolean {
            val foundSupport = getSupport(framework) ?: return false
            return foundSupport.architectures.contains(model.architecture)
        }

        fun isSupportedLanguage(model: HelloWorldModel, language: String): Boolean {
            val foundSupport = getSupport(model.framework) ?: return false
            return foundSupport.languages.contains(language)
        }

        fun isSupportedArchitecture(model: HelloWorldModel, architecture: String): Boolean {
            val foundSupport = getSupport(model.framework) ?: return false
            if (model.buildTool == "gradle") {
                return foundSupport.architectures.contains(architecture) &&
                    setOf("springboot", "servlet").contains(architecture)
            } else if (model.language == "kotlin") {
                return architecture == "springboot"
            }
            return foundSupport.architectures.contains(architecture)
        }

        fun isSupportedBuildTool(model: HelloWorldModel, buildTool: String): Boolean {
            val foundSupport = getSupport(model.framework) ?: return false
            if (model.language == "kotlin") {
                return foundSupport.buildTools.contains(buildTool) && buildTool == "maven"
            }
            return foundSupport.buildTools.contains(buildTool)
        }

        fun supportsAllArchitectures(model: HelloWorldModel): Boolean {
            return getSupport(model.framework)?.architectures?.containsAll(architectures.keys)!!
        }

        private fun getSupport(framework: String): StarterSupportMatrixElement? {
            return supportMatrix.find { it.framework == framework }
        }
    }
}
