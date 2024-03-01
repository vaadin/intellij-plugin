package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import java.io.File

open class UndoHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val copilotActionPrefix = "_Undo Vaadin Copilot"

    protected val vfsFiles: ArrayList<VirtualFile> = ArrayList()

    init {
        val paths = data["files"] as Collection<String>
        for (path in paths) {
            val file = File(path)
            if (isFileInsideProject(project, file)) {
                VfsUtil.findFileByIoFile(file, true)?.let {
                    vfsFiles.add(it)
                }
            } else {
                LOG.warn("File $file is not a part of a project")
            }
        }
    }

    override fun run() {
        for (vfsFile in vfsFiles) {
            getEditorWrapper(vfsFile).use {wrapper ->
                val undoManager = UndoManagerImpl.getInstance(project)
                val editor = wrapper.getFileEditor()
                if (undoManager.isUndoAvailable(editor)) {
                    val undo = undoManager.getUndoActionNameAndDescription(editor).first
                    if (undo.startsWith(copilotActionPrefix)) {
                        undoManager.undo(editor)
                        commitAndFlush(vfsFile.findDocument())
                        LOG.info("$undo performed on ${vfsFile.path}")
                        return
                    }
                }
            }
        }
    }

}
