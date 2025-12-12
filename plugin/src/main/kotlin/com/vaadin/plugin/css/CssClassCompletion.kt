package com.vaadin.plugin.css

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.css.CssSelectorSuffix
import com.intellij.psi.css.impl.stubs.index.CssClassIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

/**
 * Provides exclusive CSS class name completion for Vaadin Flow's HasStyle methods. Only shows CSS classes, no other
 * suggestions.
 */
class CssClassCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .inside(PsiLiteralExpression::class.java)
                .inside(PsiExpressionList::class.java)
                .inside(PsiMethodCallExpression::class.java),
            VaadinCssCompletionProvider())
    }

    private class VaadinCssCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            if (!isInTargetMethod(parameters.position)) return

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

    companion object {
        private const val HAS_STYLE_FQN = "com.vaadin.flow.component.HasStyle"

        private val TARGET_METHODS =
            setOf("addClassName", "addClassNames", "setClassName", "removeClassName", "removeClassNames")

        internal fun isInTargetMethod(position: PsiElement): Boolean {
            val literal = PsiTreeUtil.getParentOfType(position, PsiLiteralExpression::class.java) ?: return false

            val methodCall = PsiTreeUtil.getParentOfType(literal, PsiMethodCallExpression::class.java) ?: return false

            val methodName = methodCall.methodExpression.referenceName ?: return false
            if (methodName !in TARGET_METHODS) return false

            val method = methodCall.resolveMethod() ?: return false
            val containingClass = method.containingClass ?: return false

            return isHasStyleOrSubtype(containingClass)
        }

        private fun isHasStyleOrSubtype(psiClass: PsiClass): Boolean {
            if (psiClass.qualifiedName == HAS_STYLE_FQN) return true
            return psiClass.supers.any { isHasStyleOrSubtype(it) }
        }
    }
}
