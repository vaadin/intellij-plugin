package com.vaadin.plugin.css

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

/**
 * Ensures that auto-completion popup appears automatically when typing inside Vaadin's addClassName() and related
 * method string arguments.
 */
class CssCompletionConfidence : CompletionConfidence() {

    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        if (!CssClassCompletionContributor.isInTargetMethod(contextElement)) {
            return ThreeState.UNSURE
        }

        // Don't skip - we want the popup
        return ThreeState.NO
    }
}
