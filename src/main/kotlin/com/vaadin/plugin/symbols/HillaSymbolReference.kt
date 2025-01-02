package com.vaadin.plugin.symbols

import com.intellij.model.SingleTargetReference
import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.vaadin.plugin.endpoints.HILLA_BROWSER_CALLABLE

class HillaSymbolReference(private val element: PsiElement) : SingleTargetReference(), PsiSymbolReference {

    override fun resolveSingleTarget(): Symbol? {
        return CachedValuesManager.getCachedValue(element, Key.create(element.text)) {
            CachedValueProvider.Result.create(internalResolveSingleTarget(), element)
        }
    }

    private fun internalResolveSingleTarget(): Symbol? {
        val hillaBrowserCallableClass =
            JavaPsiFacade.getInstance(element.project)
                .findClass(HILLA_BROWSER_CALLABLE, ProjectScope.getLibrariesScope(element.project)) ?: return null

        val scope = GlobalSearchScope.allScope(element.project)

        var className = element.text
        var methodName: String? = null

        // method call
        if (element.textContains('.')) {
            element.text.split('.').let {
                className = it[0]
                methodName = it[1]
            }
        }

        val psiClass =
            AnnotatedElementsSearch.searchPsiClasses(hillaBrowserCallableClass, scope).firstOrNull {
                it.name?.endsWith(className) == true
            }

        if (psiClass == null) {
            return null
        }

        if (methodName != null) {
            // search also in super classes
            val psiMethod = psiClass?.findMethodsByName(methodName, true)?.firstOrNull()
            if (psiMethod != null) {
                return HillaSymbol(psiMethod)
            }
        }
        return HillaSymbol(psiClass)
    }

    override fun getElement(): PsiElement {
        return element
    }

    override fun getRangeInElement(): TextRange {
        val indexOfDot = element.text.indexOf('.')
        if (indexOfDot != -1) {
            return TextRange.from(indexOfDot + 1, element.text.length - indexOfDot)
        }
        return TextRange.allOf(element.text)
    }
}
