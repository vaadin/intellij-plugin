package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.editor.VisualPosition
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
            VfsUtil.findFileByIoFile(ioFile, true)?.let {
                val openFileDescriptor = OpenFileDescriptor(project, it)
                FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true)?.
                caretModel?.currentCaret?.moveToVisualPosition(VisualPosition(line, column))
                LOG.info("File $ioFile opened in IDE at $line:$column")
            }
        } else {
            LOG.warn("File $ioFile is not a part of a project")
        }
    }

}
