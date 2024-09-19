package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.vaadin.plugin.utils.IdeUtil
import java.io.File

class ShowInIdeHandler(project: Project, data: Map<String, Any>) :
    AbstractHandler(project) {

    private val ioFile: File = File(data["file"] as String)
    private val line: Int = (data["line"] as Int?) ?: 0
    private val column: Int = (data["column"] as Int?) ?: 0

    override fun run(): HandlerResponse {
        if (isFileInsideProject(project, ioFile)) {
            val result =
                VfsUtil.findFileByIoFile(ioFile, true)?.let { file ->
                    val openFileDescriptor = OpenFileDescriptor(project, file)
                    runInEdt {
                        FileEditorManager.getInstance(project)
                            .openTextEditor(openFileDescriptor, true)
                            ?.let { editor ->
                                editor.selectionModel.removeSelection()
                                editor.caretModel.currentCaret
                                    .moveToLogicalPosition(
                                        LogicalPosition(line, column))
                                editor.scrollingModel.scrollToCaret(
                                    ScrollType.CENTER)
                            }
                        IdeUtil.bringToFront(project)
                    }
                    LOG.info("File $ioFile opened at $line:$column")
                    true
                }
            if (result != true) {
                LOG.warn(
                    "Cannot open $ioFile at $line:$column, file does not exist or is not readable")
                return RESPONSE_ERROR
            }

            return RESPONSE_OK
        } else {
            LOG.warn("File $ioFile is not a part of a project")
            return RESPONSE_BAD_REQUEST
        }
    }
}
