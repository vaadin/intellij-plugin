package com.vaadin.plugin.css

import com.intellij.patterns.PatternCondition
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

private const val HAS_STYLE_FQN = "com.vaadin.flow.component.HasStyle"

private val TARGET_METHODS =
    setOf("addClassName", "addClassNames", "setClassName", "removeClassName", "removeClassNames")

/** Condition to check if the given PsiElement is inside a string literal argument of a HasStyle method call. */
class HasStyleReceiverCondition : PatternCondition<PsiElement>("hasStyleReceiver") {

    override fun accepts(element: PsiElement, context: ProcessingContext?): Boolean {
        val literal = PsiTreeUtil.getParentOfType(element, PsiLiteralExpression::class.java) ?: return false

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
