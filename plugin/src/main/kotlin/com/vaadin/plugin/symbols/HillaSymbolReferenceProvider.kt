package com.vaadin.plugin.symbols

import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.model.Symbol
import com.intellij.model.psi.PsiExternalReferenceHost
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceHints
import com.intellij.model.psi.PsiSymbolReferenceProvider
import com.intellij.model.search.SearchRequest
import com.intellij.openapi.project.Project

internal class HillaSymbolReferenceProvider : PsiSymbolReferenceProvider {

    override fun getReferences(
        host: PsiExternalReferenceHost,
        hints: PsiSymbolReferenceHints
    ): Collection<PsiSymbolReference> {
        if (host !is JSReferenceExpression) {
            return emptyList()
        }

        return listOf(HillaSymbolReference(host))
    }

    override fun getSearchRequests(project: Project, symbol: Symbol): Collection<SearchRequest> {
        return emptyList()
    }
}
