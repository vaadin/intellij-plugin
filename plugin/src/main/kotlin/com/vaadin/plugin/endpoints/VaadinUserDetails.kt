package com.vaadin.plugin.endpoints

import com.intellij.psi.PsiAnchor

class VaadinUserDetails(
    val className: String,
    val origin: String,
    val source: String,
    val path: String,
    val anchor: PsiAnchor,
) {
    fun isValid(): Boolean = anchor.retrieve() != null
}
