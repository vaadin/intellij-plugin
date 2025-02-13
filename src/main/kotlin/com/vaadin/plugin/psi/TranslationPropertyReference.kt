package com.vaadin.plugin.psi

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.lang.properties.references.PropertyReference
import com.intellij.psi.PsiElement

class TranslationPropertyReference(key: String, psiElement: PsiElement) :
    PropertyReference(key, psiElement, "vaadin-i18n.translations", false), HighlightedReference {}
