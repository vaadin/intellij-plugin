package com.vaadin.plugin.completion

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.ProximityLocation
import com.intellij.psi.util.proximity.ProximityWeigher
import com.vaadin.plugin.utils.hasVaadin

/**
 * Boosts Vaadin classes and members in import/completion suggestions when the same short name exists in multiple
 * packages.
 */
class VaadinImportProximityWeigher : ProximityWeigher() {

    override fun weigh(element: PsiElement, location: ProximityLocation): Comparable<*> {
        val position = location.position ?: return DEFAULT_WEIGHT
        if (!position.language.isKindOf(JavaLanguage.INSTANCE)) {
            return DEFAULT_WEIGHT
        }

        val qualifiedName = qualifiedName(element)
        if (qualifiedName?.startsWith(VAADIN_PACKAGE_PREFIX) == true &&
            hasVaadinInScope(position.project, location.positionModule)) {
            return VAADIN_WEIGHT
        }

        return DEFAULT_WEIGHT
    }
}

/**
 * Prioritizes Vaadin classes in completion lists (e.g., Ctrl+Space) when Vaadin is on the classpath.
 */
class VaadinCompletionWeigher : CompletionWeigher() {

    override fun weigh(element: LookupElement, location: CompletionLocation): Comparable<*> {
        val psiElement = element.psiElement ?: return DEFAULT_WEIGHT
        if (!psiElement.language.isKindOf(JavaLanguage.INSTANCE)) {
            return DEFAULT_WEIGHT
        }

        val qualifiedName = qualifiedName(psiElement)
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement)
        if (qualifiedName?.startsWith(VAADIN_PACKAGE_PREFIX) == true &&
            hasVaadinInScope(psiElement.project, module)) {
            return VAADIN_WEIGHT
        }

        return DEFAULT_WEIGHT
    }
}

private fun qualifiedName(element: PsiElement): String? =
    when (element) {
        is PsiClass -> element.qualifiedName
        is PsiMember -> element.containingClass?.qualifiedName
        else -> null
    }

private fun hasVaadinInScope(project: Project, module: Module?): Boolean {
    module?.let(::hasVaadin)?.let { return it }
    return hasVaadin(project)
}

private const val VAADIN_PACKAGE_PREFIX = "com.vaadin."
private const val VAADIN_WEIGHT = 0
private const val DEFAULT_WEIGHT = 1
