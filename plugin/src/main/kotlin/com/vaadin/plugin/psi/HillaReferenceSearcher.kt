package com.vaadin.plugin.psi

import com.intellij.lang.javascript.JSElementTypes
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.elementType
import com.intellij.util.Processor
import com.vaadin.plugin.endpoints.HILLA_BROWSER_CALLABLE

class HillaReferenceSearcher : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>() {

    private val typeScriptFileType = FileTypeManager.getInstance().getFileTypeByExtension("tsx")

    override fun processQuery(
        queryParameters: MethodReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val psiMethod = queryParameters.method

        ReadAction.run<Throwable> {
            if (psiMethod.containingClass?.hasAnnotation(HILLA_BROWSER_CALLABLE) != true) {
                return@run
            }

            val searchHelper = PsiSearchHelper.getInstance(queryParameters.project)
            val scope = GlobalSearchScope.projectScope(queryParameters.project)

            val filteredScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope, typeScriptFileType)
            searchHelper.processElementsWithWord(
                { psiElement, offset ->
                    if (psiElement.elementType == JSElementTypes.REFERENCE_EXPRESSION) {
                        val range = TextRange(offset, offset + psiMethod.name.length)
                        val reference =
                            object : PsiReferenceBase<PsiElement>(psiElement, range) {
                                override fun resolve(): PsiElement = psiElement
                            }
                        consumer.process(reference)
                    }
                    true // return false to stop the search early
                },
                filteredScope,
                psiMethod.name,
                UsageSearchContext.IN_CODE,
                true)
        }
    }
}
