package com.vaadin.plugin.module

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.vaadin.plugin.starter.HelloWorldModel
import com.vaadin.plugin.starter.StarterSupport

class HelloWorldPanel(private val model: HelloWorldModel) {

    private class SegmentModel(
        val values: LinkedHashMap<String, String>,
        val property: GraphProperty<String>,
        val supported: ((HelloWorldModel, String) -> Boolean)
    ) {
        var component: SegmentedButton<String>? = null

        fun reset() {
            property.set(values.keys.first())
        }

        fun value(): String {
            return property.get()
        }

        fun label(): String {
            return values[value()]!!
        }

        fun label(key: String): String {
            return values[key]!!
        }

        fun update() {
            component?.let { values.forEach { v -> it.update(v.key) } }
        }
    }

    private class ViewModel(val model: HelloWorldModel) {
        val framework =
            SegmentModel(StarterSupport.frameworks, model.frameworkProperty, StarterSupport::isSupportedFramework)
        val language =
            SegmentModel(StarterSupport.languages, model.languageProperty, StarterSupport::isSupportedLanguage)
        val buildTool =
            SegmentModel(StarterSupport.buildTools, model.buildToolProperty, StarterSupport::isSupportedBuildTool)
        val architecture =
            SegmentModel(
                StarterSupport.architectures, model.architectureProperty, StarterSupport::isSupportedArchitecture)

        fun all(): List<SegmentModel> {
            return listOf(framework, language, buildTool, architecture)
        }

        fun isSupported(segmentModel: SegmentModel, value: String): Boolean {
            return segmentModel.supported(model, value)
        }
    }

    private val viewModel: ViewModel = ViewModel(model)

    private val graph: PropertyGraph = PropertyGraph()
    private val kotlinInfoVisibleProperty = graph.property(false)
    private val notAllArchitecturesVisibleProperty = graph.property(false)
    private val notAllArchitecturesSupportedMessage = graph.property("")

    init {
        viewModel.framework.property.afterChange { refreshSegments("framework") }
        viewModel.language.property.afterChange { refreshSegments("language") }
        viewModel.buildTool.property.afterChange { refreshSegments("buildTool") }
        viewModel.architecture.property.afterChange { refreshSegments("architecture") }
        notAllArchitecturesSupportedMessage.afterChange { notAllArchitecturesVisibleProperty.set(it != "") }
    }

    private fun buildSegment(row: Row, segmentModel: SegmentModel) {
        segmentModel.component =
            row.segmentedButton(segmentModel.values.keys) {
                    this.text = segmentModel.label(it)
                    this.enabled = viewModel.isSupported(segmentModel, it)
                }
                .bind(segmentModel.property)
    }

    val root: DialogPanel = panel {
        buildSegment(row("Framework") {}, viewModel.framework)
        buildSegment(row("Language") {}, viewModel.language)
        row("") { text("<i>Kotlin support uses a community add-on.</i>") }.visibleIf(kotlinInfoVisibleProperty)
        buildSegment(row("Build tool") {}, viewModel.buildTool)
        buildSegment(row("Architecture") {}, viewModel.architecture)
        row("") { text("").bindText(notAllArchitecturesSupportedMessage) }.visibleIf(notAllArchitecturesVisibleProperty)
    }

    private fun refreshSegments(source: String) {
        if (source != "framework") {
            viewModel.framework.update()
        }
        if (source != "language") {
            viewModel.language.update()
        }
        if (source != "buildTool") {
            viewModel.buildTool.update()
        }
        if (source != "architecture") {
            viewModel.architecture.update()
        }
        refreshArchitecturesSupportedMessage()
        refreshKotlinInfo()
        fallbackToFirstEnabled()
    }

    private fun refreshKotlinInfo() {
        kotlinInfoVisibleProperty.set(viewModel.language.value() == "kotlin")
    }

    private fun refreshArchitecturesSupportedMessage() {
        if (StarterSupport.supportsAllArchitectures(model)) {
            notAllArchitecturesSupportedMessage.set("")
        } else {
            val frameworkName = viewModel.framework.label()
            notAllArchitecturesSupportedMessage.set("<i>$frameworkName does not support all architectures.</i>")
        }
    }

    private fun fallbackToFirstEnabled() {
        viewModel.all().filter { !viewModel.isSupported(it, it.value()) }.forEach { it.reset() }
    }
}
