package com.vaadin.plugin.psi

import com.intellij.lang.javascript.patterns.JSPatterns
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

class HillaReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            JSPatterns.jsArgument("translate", 0),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    if (element !is JSLiteralExpression) return PsiReference.EMPTY_ARRAY
                    if (!(element.containingFile as JSFile).referencedPaths.contains("@vaadin/hilla-react-i18n"))
                        return PsiReference.EMPTY_ARRAY
                    return arrayOf(TranslationPropertyReference(element.value as String, element))
                }
            })
    }
}
