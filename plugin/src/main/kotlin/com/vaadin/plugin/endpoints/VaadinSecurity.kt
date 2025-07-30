package com.vaadin.plugin.endpoints

import com.intellij.psi.PsiAnchor

class VaadinSecurity(
    val className: String,
    val origin: String,
    val source: String,
    val path: String,
    val anchor: PsiAnchor,
    val loginView: String? = null,
) {
    fun isValid(): Boolean = anchor.retrieve() != null
}
