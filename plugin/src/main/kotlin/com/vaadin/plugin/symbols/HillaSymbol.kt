package com.vaadin.plugin.symbols

import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.navigation.NavigatableSymbol
import com.intellij.navigation.SymbolNavigationService
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.navigation.NavigationTarget
import com.intellij.psi.PsiElement

class HillaSymbol(private val target: PsiElement) : NavigatableSymbol {

    override fun createPointer(): Pointer<out Symbol> {
        return Pointer.hardPointer(this)
    }

    override fun getNavigationTargets(project: Project): Collection<NavigationTarget> {
        if (target.project != project) return emptyList()
        return listOf(SymbolNavigationService.getInstance().psiElementNavigationTarget(target))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HillaSymbol

        return target == other.target
    }

    override fun hashCode(): Int {
        return target.hashCode()
    }
}
