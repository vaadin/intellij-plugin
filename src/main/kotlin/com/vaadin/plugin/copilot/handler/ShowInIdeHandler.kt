package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File


class ShowInIdeHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val ioFile: File = File(data["file"] as String)
    private val line: Int = (data["line"] as Int?) ?: 0
    private val column: Int = (data["column"] as Int?) ?: 0

    private val vfsFile: VirtualFile?

    init {
        if (isFileInsideProject(project, ioFile)) {
            vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
        } else {
            LOG.warn("File $ioFile is not a part of a project")
            vfsFile = null
        }
    }

    override fun run() {
        if (vfsFile != null) {
            val openFileDescriptor = OpenFileDescriptor(project, vfsFile)
            FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true)?.
                caretModel?.currentCaret?.moveToVisualPosition(VisualPosition(line, column))
            LOG.info("File $ioFile opened in IDE at $line:$column")
        } else {
            LOG.warn("Cannot open not existing $ioFile")
        }
    }

}