package com.vaadin.plugin.listeners

import com.intellij.codeInsight.JavaProjectCodeInsightSettings
import com.intellij.openapi.project.Project

private const val JAVA_AWT_PACKAGE = "java.awt"

/** Adds java.awt to the excluded import/completion list for Vaadin projects to avoid suggesting AWT widgets. */
class VaadinImportExclusionListener : VaadinProjectListener {

    override fun vaadinProjectDetected(project: Project) {
        if (project.isDisposed) {
            return
        }
        val excluded = JavaProjectCodeInsightSettings.getSettings(project).excludedNames
        if (JAVA_AWT_PACKAGE !in excluded && "$JAVA_AWT_PACKAGE.*" !in excluded) {
            excluded.add(JAVA_AWT_PACKAGE)
        }
    }
}
