package com.vaadin.plugin.starter

class StarterSupport {

    companion object {

        val frameworks = linkedMapOf(
            "24" to "Vaadin Flow 24",
            "23" to "Vaadin Flow 23",
            "14" to "Vaadin Flow 14",
            "hilla-react" to "Hilla + React",
            "hilla-lit" to "Hilla + Lit"
        )

        val languages = linkedMapOf(
            "17" to "Java",
            "kotlin" to "Kotlin"
        )

        val architectures = linkedMapOf(
            "springboot" to "Spring Boot",
            "quarkus" to "Quarkus",
            "jakartaee" to "Jakarta EE",
            "servlet" to "Servlet",
            "osgi" to "OSGI",
            "karaf" to "Karaf"
        )

        val buildTools = linkedMapOf(
            "maven" to "Maven",
            "gradle" to "Gradle"
        )

        val downloadLinks = mapOf(
            "hilla-react" to "https://github.com/vaadin/skeleton-starter-hilla-react/archive/master.zip",
            "hilla-lit" to "https://github.com/vaadin/skeleton-starter-hilla-lit/archive/master.zip",
            "hilla-react-gradle" to "https://github.com/vaadin/skeleton-starter-hilla-react-gradle/archive/master.zip",
            "hilla-lit-gradle" to "https://github.com/vaadin/skeleton-starter-hilla-lit-gradle/archive/master.zip",
            "kotlin" to "https://github.com/vaadin/skeleton-starter-kotlin-spring/archive/master.zip",
            "gradle-servlet" to "https://github.com/vaadin/base-starter-gradle/archive/v<version>.zip",
            "gradle-springboot" to "https://github.com/vaadin/base-starter-spring-gradle/archive/v<version>.zip",
            "springboot" to "https://github.com/vaadin/skeleton-starter-flow-spring/archive/v<version>.zip",
            "quarkus" to "https://github.com/vaadin/base-starter-flow-quarkus/archive/v<version>.zip",
            "jakartaee" to "https://github.com/vaadin/skeleton-starter-flow-cdi/archive/v<version>.zip",
            "servlet" to "https://github.com/vaadin/skeleton-starter-flow/archive/v<version>.zip",
            "osgi" to "https://github.com/vaadin/base-starter-flow-osgi/archive/v<version>.zip",
            "karaf" to "https://github.com/vaadin/vaadin-flow-karaf-example/archive/v<version>.zip"
        )

        private val supportMatrix = arrayOf(
            StarterSupportMatrixElement(
                "24",
                languages.keys,
                setOf("springboot", "quarkus", "jakartaee", "servlet"),
                buildTools.keys,
                17
            ),
            StarterSupportMatrixElement(
                "23",
                setOf("17"),
                architectures.keys,
                buildTools.keys,
                11
            ),
            StarterSupportMatrixElement(
                "14",
                setOf("17"),
                setOf("springboot", "jakartaee", "servlet", "osgi"),
                buildTools.keys,
                8
            ),
            StarterSupportMatrixElement(
                "hilla-react",
                setOf("17"),
                setOf("springboot"),
                buildTools.keys,
                17
            ),
            StarterSupportMatrixElement(
                "hilla-lit",
                setOf("17"),
                setOf("springboot"),
                buildTools.keys,
                17
            )
        )


        fun isSupportedFramework(model: StarterModel, framework: String): Boolean {
            val foundSupport = getSupport(framework) ?: return false
            return try {
                (Integer.parseInt(model.language) >= foundSupport.javaMinVersion
                        && foundSupport.architectures.contains(model.architecture))
            } catch (e: NumberFormatException) {
                true // kotlin
            }
        }

        fun isSupportedLanguage(model: StarterModel, language: String): Boolean {
            val foundSupport = getSupport(model.framework) ?: return false
            return foundSupport.languages.contains(language)
        }

        fun isSupportedArchitecture(model: StarterModel, architecture: String): Boolean {
            val foundSupport = getSupport(model.framework) ?: return false
            if (model.buildTool == "gradle") {
                return foundSupport.architectures.contains(architecture) && setOf("springboot", "servlet").contains(
                    architecture
                )
            } else if (model.language == "kotlin") {
                return architecture == "springboot"
            }
            return foundSupport.architectures.contains(architecture)
        }

        fun isSupportedBuildTool(model: StarterModel, buildTool: String): Boolean {
            val foundSupport = getSupport(model.framework) ?: return false
            if (model.language == "kotlin") {
                return foundSupport.buildTools.contains(buildTool) && buildTool == "maven"
            }
            return foundSupport.buildTools.contains(buildTool)
        }

        fun supportsAllArchitectures(model: StarterModel): Boolean {
            return getSupport(model.framework)?.architectures?.containsAll(architectures.keys)!!
        }

        private fun getSupport(framework: String): StarterSupportMatrixElement? {
            return supportMatrix.find { it.framework == framework }
        }

    }

}
