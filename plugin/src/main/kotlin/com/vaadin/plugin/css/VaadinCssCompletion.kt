package com.vaadin.plugin.css

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.vaadin.plugin.utils.hasVaadin

/** Adds Vaadin CSS variables, component selectors, and component parts to CSS completion. */
class VaadinCssCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), VaadinCssCompletionProvider())
    }
}

private class VaadinCssCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project
        if (!hasVaadin(project)) return

        val textBeforeCaret = parameters.originalFile.text.substring(0, parameters.offset)
        val catalog = VaadinCssCatalog.load()
        val componentName = partComponent(textBeforeCaret)
        when {
            isVariableContext(textBeforeCaret) -> addVariables(catalog, result)
            componentName != null -> addParts(catalog, componentName, result)
            isSelectorContext(textBeforeCaret) -> addComponents(catalog, result)
        }
    }

    private fun addVariables(catalog: VaadinCssCatalog, result: CompletionResultSet) {
        catalog.variables.forEach { variable ->
            result.addElement(
                LookupElementBuilder.create(variable.name)
                    .withIcon(AllIcons.FileTypes.Css)
                    .withTypeText("Vaadin CSS variable"))
        }
    }

    private fun addComponents(catalog: VaadinCssCatalog, result: CompletionResultSet) {
        catalog.components.forEach { component ->
            result.addElement(
                LookupElementBuilder.create(component.name)
                    .withIcon(AllIcons.Xml.Css_class)
                    .withTypeText("Vaadin component"))
        }
    }

    private fun addParts(catalog: VaadinCssCatalog, componentName: String, result: CompletionResultSet) {
        catalog.components
            .firstOrNull { it.name == componentName }
            ?.parts
            ?.forEach { part ->
                result.addElement(
                    LookupElementBuilder.create(part.name)
                        .withIcon(AllIcons.Xml.Css_class)
                        .withTypeText("$componentName part"))
            }
    }

    private fun isVariableContext(text: String): Boolean =
        Regex("(?:var\\(\\s*)?--[a-zA-Z0-9-]*$").containsMatchIn(text)

    private fun partComponent(text: String): String? =
        Regex("(vaadin-[a-z0-9-]+)[^{}]*::part\\(\\s*[a-z0-9-]*$", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.get(1)

    private fun isSelectorContext(text: String): Boolean {
        val currentBlock = text.substringAfterLast('{')
        return ':' !in currentBlock && ';' !in currentBlock
    }
}
