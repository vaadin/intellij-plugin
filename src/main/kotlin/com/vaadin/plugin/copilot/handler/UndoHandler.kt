package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.psi.PsiDocumentManager
import com.vaadin.plugin.copilot.service.CopilotFileTrackingService

open class UndoHandler(private val project: Project) : Runnable {

    private val copilotActionPrefix = "_Undo Copilot"

    override fun run() {
        val vfsFile = project.service<CopilotFileTrackingService>().getLastModified() ?: return
        val editor = getEditor(vfsFile)

        val undoManager = UndoManagerImpl.getInstance(project)
        if (undoManager.isUndoAvailable(editor)) {
            val undo = undoManager.getUndoActionNameAndDescription(editor).first
            if (undo.startsWith(copilotActionPrefix)) {
                undoManager.undo(editor)
                commitAndFlush(vfsFile)
            }
        }
    }

    fun getEditor(vfsFile: VirtualFile): FileEditor? {
        val editors = FileEditorManager.getInstance(project).getEditors(vfsFile)

        if (editors.isEmpty()) {
            return null
        }
        return editors.first()
    }

    fun commitAndFlush(vfsFile: VirtualFile) {
        val vfsDoc = vfsFile.findDocument()
        if (vfsDoc != null) {
            PsiDocumentManager.getInstance(project).commitDocument(vfsDoc)
            FileDocumentManager.getInstance().saveDocument(vfsDoc)
        }
    }

}