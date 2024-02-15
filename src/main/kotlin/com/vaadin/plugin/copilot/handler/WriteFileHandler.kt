package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiDocumentManager
import com.vaadin.plugin.copilot.service.CopilotFileTrackingService
import java.io.File
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class WriteFileHandler(private val project: Project, data: Map<String, Any>) : Runnable {

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
                        PsiDocumentManager.getInstance(project).commitDocument(vfsDoc)
                        FileDocumentManager.getInstance().saveDocument(vfsDoc)
                        project.service<CopilotFileTrackingService>().setLastModified(vfsFile!!)
                    }
                },
                undoLabel ?: "Copilot Write File",
                DocCommandGroupId.noneGroupId(vfsDoc),
                UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
            )
        }
    }

    @Throws(IOException::class)
    fun isFileInsideProject(project: Project, file: File): Boolean {
        val path = getRealPath(file)
        return (path.startsWith(project.basePath))
    }

    @Throws(IOException::class)
    private fun getRealPath(file: File): Path {
        val path = file.toPath()
        return try {
            path.toRealPath()
        } catch (e: NoSuchFileException) {
            // As we allow creating new files, we check the directory instead
            path.parent.toRealPath().resolve(path.fileName)
        }
    }

}