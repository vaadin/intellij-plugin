package com.vaadin.plugin.completion

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Key
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
        if (!isJavaContext(location)) {
            return DEFAULT_WEIGHT
        }

        val qualifiedName = getQualifiedName(element)
        if (qualifiedName?.startsWith(VAADIN_PACKAGE_PREFIX) == true && hasVaadinInScope(location)) {
            return VAADIN_WEIGHT
        }

        return DEFAULT_WEIGHT
    }

    private fun isJavaContext(location: ProximityLocation): Boolean {
        val position = location.position ?: return false
        return position.language.isKindOf(JavaLanguage.INSTANCE)
    }

    private fun hasVaadinInScope(location: ProximityLocation): Boolean {
        location.getUserData(HAS_VAADIN_KEY)?.let {
            return it
        }

        val project = location.project ?: return false
        val hasVaadin = location.positionModule?.let(::hasVaadin) ?: hasVaadin(project)

        location.putUserData(HAS_VAADIN_KEY, hasVaadin)
        return hasVaadin
    }

    private fun getQualifiedName(element: PsiElement): String? =
        when (element) {
            is PsiClass -> element.qualifiedName
            is PsiMember -> element.containingClass?.qualifiedName
            else -> null
        }

    companion object {
        private const val VAADIN_PACKAGE_PREFIX = "com.vaadin."
        private const val VAADIN_WEIGHT = 0
        private const val DEFAULT_WEIGHT = 1
        private val HAS_VAADIN_KEY = Key.create<Boolean>("com.vaadin.plugin.imports.hasVaadin")
    }
}
