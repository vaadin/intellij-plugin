package com.vaadin.plugin.copilot.handler

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.io.File


class ShowInIdeHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val ioFile: File = File(data["file"] as String)
    private val line: Int = (data["line"] as Int?) ?: 0
    private val column: Int = (data["column"] as Int?) ?: 0

    override fun run() {
        if (isFileInsideProject(project, ioFile)) {
            val result = VfsUtil.findFileByIoFile(ioFile, true)?.let { file ->
                val openFileDescriptor = OpenFileDescriptor(project, file)
                FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true)?.let { editor ->
                    editor.selectionModel.removeSelection()
                    editor.caretModel.currentCaret.moveToLogicalPosition(LogicalPosition(line, column))
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
                ProjectUtil.focusProjectWindow(project, true)
                LOG.info("File $ioFile opened at $line:$column")
                true
            }
            if (result != true) {
                LOG.warn("Cannot open $ioFile at $line:$column, file does not exist or is not readable")
            }
        } else {
            LOG.warn("File $ioFile is not a part of a project")
        }
    }

}
