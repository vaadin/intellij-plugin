package com.vaadin.plugin.endpoints

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.util.ProcessingContext

class HillaReferenceContributor : PsiReferenceContributor() {

    internal class TsxToJavaReferenceProvider : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            if (element.parent is JSCallExpression) {
                val methodExpression = (element.parent as JSCallExpression).methodExpression
                val className = methodExpression?.firstChild?.text
                val methodName = methodExpression?.lastChild?.text

                if (className != null && methodName != null) {
                    return arrayOf(TsxToJavaPsiReference(element, className, methodName))
                }
            }
            return PsiReference.EMPTY_ARRAY
        }
    }

    internal class TsxToJavaPsiReference(
        element: PsiElement,
        private val className: String,
        private val methodName: String
    ) : PsiReferenceBase<PsiElement>(element) {

        override fun resolve(): PsiElement? {
            val project = element.project
            val scope = GlobalSearchScope.allScope(project)
            val psiClass = AllClassesSearch.search(scope, project) { it.endsWith(className) }.findFirst()

            if (psiClass != null) {
                return psiClass.findMethodsByName(methodName, true).firstOrNull()
            }
            return null
        }

        override fun getVariants(): Array<Any> {
            // Optionally provide completion variants
            return emptyArray()
        }
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JSReferenceExpression::class.java), TsxToJavaReferenceProvider())
    }
}
