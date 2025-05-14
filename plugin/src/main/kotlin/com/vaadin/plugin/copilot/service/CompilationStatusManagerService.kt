package com.vaadin.plugin.copilot.service

interface CompilationStatusManagerService {

    /**
     * Subscribes to project message bus to listen compilation results
     */
    fun init()
    /**
     * Retrieves the set of file names that caused compilation errors for the project
     *
     * @return A [Set] of file paths that had errors, or an empty set if none are recorded.
     */
    fun getErrorFiles(): Set<String>

    /**
     * Checks if the project has compilation errors.
     *
     * @return `true` if the project has errors, otherwise `false`.
     */
    fun hasCompilationError(): Boolean
}