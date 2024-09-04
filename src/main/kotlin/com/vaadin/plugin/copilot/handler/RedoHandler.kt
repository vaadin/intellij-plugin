package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findDocument
import com.vaadin.plugin.actions.VaadinCompileOnSaveAction

class RedoHandler(project: Project, data: Map<String, Any>) : UndoHandler(project, data) {

    private val copilotActionPrefix = "_Redo Vaadin Copilot"

    override fun run() {
        for (vfsFile in vfsFiles) {
            runInEdt {
                getEditorWrapper(vfsFile).use { wrapper ->
                    val editor = wrapper.getFileEditor()
                    val undoManager = UndoManagerImpl.getInstance(project)
                    runWriteAction {
                        if (undoManager.isRedoAvailable(editor)) {
                            val redo = undoManager.getRedoActionNameAndDescription(editor).first
                            if (redo.startsWith(copilotActionPrefix)) {
                                undoManager.redo(editor)
                                commitAndFlush(vfsFile.findDocument())
                                LOG.info("$redo performed on ${vfsFile.path}")
                                VaadinCompileOnSaveAction().compile(project, vfsFile)
                            }
                        }
                    }
                }
            }
        }
    }

}
