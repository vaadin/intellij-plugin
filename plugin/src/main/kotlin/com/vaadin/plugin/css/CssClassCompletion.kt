package com.vaadin.plugin.css

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.css.CssSelectorSuffix
import com.intellij.psi.css.impl.stubs.index.CssClassIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.ProcessingContext

/**
 * Provides exclusive CSS class name completion for Vaadin Flow's HasStyle methods. Only shows CSS classes, no other
 * suggestions.
 */
class CssClassCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().with(HasStyleReceiverCondition()),
            CssClassCompletionProvider())
    }
}

private class CssClassCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // Add all CSS class names from IntelliJ's built-in index
        val project = parameters.originalFile.project
        val scope = GlobalSearchScope.projectScope(project)
        val allClassNames = StubIndex.getInstance().getAllKeys(CssClassIndex.KEY, project)

        for (className in allClassNames) {
            val classes =
                StubIndex.getElements(CssClassIndex.KEY, className, project, scope, CssSelectorSuffix::class.java)
            if (classes.isNotEmpty()) {
                val sourceFile = classes.firstOrNull()?.containingFile?.name ?: "CSS"
                result.addElement(
                    LookupElementBuilder.create(className)
                        .withIcon(AllIcons.Xml.Css_class)
                        .withTypeText(sourceFile)
                        .withCaseSensitivity(false))
            }
        }

        // Stop other contributors from adding suggestions, we only want class names
        result.stopHere()
    }
}
