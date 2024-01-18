package com.vaadin.plugin.copilot.handlers

import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.CopilotPlugin

class UndoHandler(private val project: Project) : CopilotPlugin.CommandHandler {

    override fun handle() {
        val undoManager = UndoManagerImpl.getInstance(project)
        if (undoManager.isUndoAvailable(null)) {
            undoManager.undo(null)
        }
    }

}