package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import java.io.File

class WriteFileHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val content: String = data["content"] as String
    private val undoLabel: String? = data["undoLabel"] as String?
    private val vfsDoc: Document?
    private val vfsFile: VirtualFile?

    init {
        val file = File(data["file"] as String)
        if (!isFileInsideProject(project, file)) {
            throw Exception("File is not a part of a project")
        }
        vfsFile = VfsUtil.findFileByIoFile(file, true)
        vfsDoc = vfsFile?.findDocument()
    }

    override fun run() {
        if (vfsDoc != null && ReadonlyStatusHandler.ensureDocumentWritable(project, vfsDoc)) {
            CommandProcessor.getInstance().executeCommand(
                project,
                {
                    WriteCommandAction.runWriteCommandAction(project) {
                        vfsDoc.setText(content)
                        commitAndFlush(vfsDoc)
                    }
                },
                undoLabel ?: "Copilot Write File",
                DocCommandGroupId.noneGroupId(vfsDoc),
                UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
            )
        }
    }


}
