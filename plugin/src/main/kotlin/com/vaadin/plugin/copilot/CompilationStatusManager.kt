package com.vaadin.plugin.copilot

import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

object CompilationStatusManager {
    private val projectStatusMap: MutableMap<Project, Boolean> = ConcurrentHashMap()

    fun setCompilationFailure(project: Project, hasFailure: Boolean) {
        projectStatusMap[project] = hasFailure
    }
    fun hasCompilationFailure(project: Project): Boolean {
        return projectStatusMap[project] ?: false
    }
}