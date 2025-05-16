package com.vaadin.plugin.copilot.service

import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.compiler.CompilerTopics
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CompilationStatusManagerService(private val project: Project) : CompilationStatusListener {

    /**
     * Holds compilation error state for a given project, including:
     * - [hasCompilationError]: whether the last compilation attempt had errors
     * - [errorFiles]: a set of file paths (as strings) that caused the compilation errors
     */
    data class CompilationErrorInfo(
        var hasCompilationError: Boolean = false,
        val errorFiles: MutableSet<String> = mutableSetOf()
    )

    private var compilationErrorInfo: CompilationErrorInfo? = null

    /** Subscribes to project message bus to listen compilation results */
    fun subscribeToCompilationStatus() {
        project.messageBus.connect().subscribe(CompilerTopics.COMPILATION_STATUS, this)
    }

    override fun compilationFinished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
        val filePaths = mutableSetOf<String>()
        if (errors > 0) {
            val messages = compileContext.getMessages(CompilerMessageCategory.ERROR)
            for (message in messages) {
                message.virtualFile?.let { virtualFile -> filePaths.add(virtualFile.path) }
            }
        }
        setHasCompilationError(errors > 0, filePaths)
        super.compilationFinished(aborted, errors, warnings, compileContext)
    }

    /**
     * Checks if the given [project] currently has compilation errors.
     *
     * @return `true` if the project has errors, otherwise `false`.
     */
    fun hasCompilationError(): Boolean {
        return compilationErrorInfo?.hasCompilationError ?: false
    }

    /**
     * Retrieves the set of file names that caused compilation errors for the given [project].
     *
     * @return A [Set] of file paths that had errors, or an empty set if none are recorded.
     */
    fun getErrorFiles(): Set<String> {
        return compilationErrorInfo?.errorFiles ?: emptySet()
    }

    /**
     * Updates the compilation error status for the given [project].
     *
     * @param hasError Whether the project has compilation errors.
     * @param files The set of file names (or paths) that caused compilation errors.
     */
    private fun setHasCompilationError(hasError: Boolean, files: Set<String> = emptySet()) {
        val info = compilationErrorInfo ?: CompilationErrorInfo()
        info.hasCompilationError = hasError
        info.errorFiles.clear()
        info.errorFiles.addAll(files)
        this.compilationErrorInfo = info
    }
}
