package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class RedoHandler(project: Project, data: Map<String, Any>) : UndoHandler(project, data) {

    override fun getOpsCount(vfsFile: VirtualFile): Int {
        return getCopilotUndoManager().getRedoCount(vfsFile)
    }

    override fun before(vfsFile: VirtualFile) {
        getCopilotUndoManager().redoStart(vfsFile)
    }

    override fun runManagerAction(undoManager: UndoManager, editor: FileEditor) {
        undoManager.redo(editor)
    }

    override fun after(vfsFile: VirtualFile) {
        getCopilotUndoManager().redoDone(vfsFile)
        LOG.info("$vfsFile redo performed")
    }
}
