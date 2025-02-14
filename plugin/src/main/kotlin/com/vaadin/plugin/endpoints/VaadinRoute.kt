package com.vaadin.plugin.endpoints

import com.intellij.psi.PsiAnchor

class VaadinRoute(val urlMapping: String, val locationString: String, val anchor: PsiAnchor) {
    fun isValid(): Boolean = anchor.retrieve() != null
}
