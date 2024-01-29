package com.vaadin.plugin.copilot.handlers

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.impl.DocumentUndoProvider
import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.vaadin.plugin.copilot.CommandHandler
import java.io.File
import java.io.IOException

class WriteHandler(project: Project, data: Map<String, Any>) : CommandHandler, UndoableAction {

    private var originalContent: String
    private val content: String = data["content"] as String
    private val vfsFile: VirtualFile?
    private val vfsDoc: Document?

    init {
        val ioFile = File(project.basePath + File.separator + data["file"])
        vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
        vfsDoc = vfsFile?.findDocument()
        originalContent = vfsFile?.readText().toString()
    }

    override fun handle() {
        redo()
    }

    override fun undo() {
        if (vfsFile?.isWritable == true) {
            DocumentUndoProvider.startDocumentUndo(vfsDoc)
            WriteAction.run<IOException> {
                vfsFile.writeText(originalContent)
            }
            DocumentUndoProvider.finishDocumentUndo(vfsDoc)
        }
    }

    override fun redo() {
        WriteAction.run<IOException> {
            if (vfsFile?.isWritable == true) {
                vfsFile.writeText(content)
            }
        }
    }

    override fun getAffectedDocuments(): Array<DocumentReference> {
        if (vfsDoc != null) {
            return arrayOf(DocumentReferenceManager.getInstance().create(vfsDoc))
        } else {
            return emptyArray()
        }
    }

    override fun isGlobal(): Boolean {
        return true
    }

}