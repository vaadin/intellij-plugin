package com.vaadin.plugin.copilot

import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton object that tracks compilation error status per IntelliJ [Project].
 *
 * It stores whether a project has compilation errors and which specific files caused them.
 */
object CompilationStatusManager {
    /**
     * Holds compilation error state for a given project, including:
     * - [hasCompilationError]: whether the last compilation attempt had errors
     * - [errorFiles]: a set of file paths (as strings) that caused the compilation errors
     */
    data class CompilationErrorInfo(
        var hasCompilationError: Boolean = false,
        val errorFiles: MutableSet<String> = mutableSetOf()
    )

    private val projectStatusMap: MutableMap<Project, CompilationErrorInfo> = ConcurrentHashMap()

    /**
     * Updates the compilation error status for the given [project].
     *
     * @param project The IntelliJ [Project] to update.
     * @param hasError Whether the project has compilation errors.
     * @param files The set of file names (or paths) that caused compilation errors.
     */
    fun setHasCompilationError(project: Project, hasError: Boolean, files: Set<String> = emptySet()) {
        val info = projectStatusMap.computeIfAbsent(project) { CompilationErrorInfo() }
        info.hasCompilationError = hasError
        info.errorFiles.clear()
        info.errorFiles.addAll(files)
    }
    /**
     * Checks if the given [project] currently has compilation errors.
     *
     * @param project The IntelliJ [Project] to check.
     * @return `true` if the project has errors, otherwise `false`.
     */
    fun hasCompilationError(project: Project): Boolean {
        return projectStatusMap[project]?.hasCompilationError ?: false
    }
    /**
     * Retrieves the set of file names that caused compilation errors for the given [project].
     *
     * @param project The IntelliJ [Project] to query.
     * @return A [Set] of file paths that had errors, or an empty set if none are recorded.
     */
    fun getErrorFiles(project: Project): Set<String> {
        return projectStatusMap[project]?.errorFiles ?: emptySet()
    }
}
