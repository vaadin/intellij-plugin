package com.vaadin.plugin.css

import org.json.JSONObject

data class VaadinCssVariable(val name: String)

data class VaadinCssPart(val name: String)

data class VaadinCssComponent(val name: String, val parts: List<VaadinCssPart>)

data class VaadinCssCatalog(val variables: List<VaadinCssVariable>, val components: List<VaadinCssComponent>) {

    companion object {
        private const val RESOURCE_PATH = "/vaadin/css-catalog.json"

        fun load(): VaadinCssCatalog {
            val resource = VaadinCssCatalog::class.java.getResourceAsStream(RESOURCE_PATH)
                ?: return VaadinCssCatalog(emptyList(), emptyList())
            resource.bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                val variables = (0 until json.getJSONArray("variables").length()).map { index ->
                    VaadinCssVariable(json.getJSONArray("variables").getJSONObject(index))
                }
                val components = (0 until json.getJSONArray("components").length()).map { index ->
                    VaadinCssComponent(json.getJSONArray("components").getJSONObject(index))
                }
                return VaadinCssCatalog(variables, components)
            }
        }

        private fun VaadinCssVariable(json: JSONObject) = VaadinCssVariable(json.getString("name"))

        private fun VaadinCssComponent(json: JSONObject): VaadinCssComponent {
            val parts = (0 until json.getJSONArray("parts").length()).map { index ->
                VaadinCssPart(json.getJSONArray("parts").getJSONObject(index))
            }
            return VaadinCssComponent(json.getString("name"), parts)
        }

        private fun VaadinCssPart(json: JSONObject) = VaadinCssPart(json.getString("name"))
    }
}
