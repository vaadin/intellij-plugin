package com.vaadin.plugin.endpoints

import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.PsiAnchor

class VaadinComponent(
    val className: String,
    val origin: String,
    val source: String,
    val path: String,
    val anchor: PsiAnchor,
    val visibleMethods: Collection<HierarchicalMethodSignature?>
) {
    fun isValid(): Boolean = anchor.retrieve() != null
}
