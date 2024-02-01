package com.vaadin.plugin.copilot.handlers

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiDocumentManager
import java.io.File
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class WriteFileAction(private val project: Project, data: Map<String, Any>) : Runnable {

    private val content: String = data["content"] as String
    private val vfsFile: VirtualFile?
    private val vfsDoc: Document?

    init {
        val file = File(data["file"] as String)
        if (!isFileInsideProject(project, file)) {
            throw Exception("File is not a part of a project")
        }
        vfsFile = VfsUtil.findFileByIoFile(file, true)
        vfsDoc = vfsFile?.findDocument()
    }

    override fun run() {
        if (vfsDoc != null) {
            CommandProcessor.getInstance().executeCommand(
                project,
                {
                    WriteCommandAction.runWriteCommandAction(project) {
                        if (vfsFile != null && vfsFile.isWritable) {
                            vfsFile.writeText(content)
                            PsiDocumentManager.getInstance(project).commitDocument(vfsDoc)
                        }
                    }
                },
                "Copilot Write File",
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