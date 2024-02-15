package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.service.CopilotFileTrackingService

class RedoHandler(val project: Project) : UndoHandler(project) {

    private val copilotActionPrefix = "_Redo Copilot"

    override fun run() {
        val vfsFile = project.service<CopilotFileTrackingService>().getLastModified() ?: return
        val editor = getEditor(vfsFile)

        val undoManager = UndoManagerImpl.getInstance(project)
        if (undoManager.isRedoAvailable(editor)) {
            val undo = undoManager.getRedoActionNameAndDescription(editor).first
            if (undo.startsWith(copilotActionPrefix)) {
                undoManager.redo(editor)
                commitAndFlush(vfsFile)
            }
        }
    }

}