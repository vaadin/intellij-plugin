package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findDocument

class RedoHandler(project: Project, data: Map<String, Any>) : UndoHandler(project, data) {

    private val copilotActionPrefix = "_Redo Copilot"

    override fun run() {
        for (vfsFile in vfsFiles) {
            getEditorWrapper(vfsFile).use { wrapper ->
                val editor = wrapper.getFileEditor()
                val undoManager = UndoManagerImpl.getInstance(project)
                if (undoManager.isRedoAvailable(editor)) {
                    val undo = undoManager.getRedoActionNameAndDescription(editor).first
                    if (undo.startsWith(copilotActionPrefix)) {
                        undoManager.redo(editor)
                        commitAndFlush(vfsFile.findDocument())
                        return
                    }
                }
            }
        }
    }

}
