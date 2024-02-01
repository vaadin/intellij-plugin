package com.vaadin.plugin.copilot.handlers

import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class UndoHandler(private val project: Project) : Runnable {

    override fun run() {
        val undoManager = UndoManagerImpl.getInstance(project)
        val editor = FileEditorManager.getInstance(project).selectedEditor
        if (undoManager.isUndoAvailable(editor)) {
            undoManager.undo(editor)
        }
    }

}