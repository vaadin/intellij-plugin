package com.vaadin.plugin.copilot

import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

object CompilationStatusManager {
    private val projectStatusMap: MutableMap<Project, Boolean> = ConcurrentHashMap()

    fun setHasCompilationError(project: Project, hasError: Boolean) {
        projectStatusMap[project] = hasError
    }

    fun hasCompilationError(project: Project): Boolean {
        return projectStatusMap[project] ?: false
    }
}
