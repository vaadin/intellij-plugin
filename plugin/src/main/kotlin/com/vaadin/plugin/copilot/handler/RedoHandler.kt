package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.vaadin.plugin.copilot.service.CopilotUndoManager.Batch

class RedoHandler(project: Project, data: Map<String, Any>) : UndoHandler(project, data) {

    override fun getBatch(vfsFile: VirtualFile): Batch? {
        return getCopilotUndoManager().peekRedoBatch(vfsFile)
    }

    /** Replays exactly the commands the matching undo reverted. */
    override fun runManagerActions(undoManager: UndoManager, editor: FileEditor, batch: Batch): Int {
        var commandCount = 0
        while (commandCount < batch.commandCount && undoManager.isRedoAvailable(editor)) {
            undoManager.redo(editor)
            commandCount++
        }
        return commandCount
    }

    override fun actionsPerformed(vfsFile: VirtualFile, commandCount: Int) {
        getCopilotUndoManager().redoPerformed(vfsFile)
        LOG.info("$vfsFile redo performed, $commandCount command(s) replayed")
    }

    override fun actionsUnavailable(vfsFile: VirtualFile) {
        getCopilotUndoManager().redoUnavailable(vfsFile)
    }
}
