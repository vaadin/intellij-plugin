package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import java.io.File

open class UndoHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    private val vfsFiles: ArrayList<VirtualFile> = ArrayList()

    init {
        val paths = data["files"] as Collection<String>
        for (path in paths) {
            val file = File(path)
            if (isFileInsideProject(project, file)) {
                var vfsFile = VfsUtil.findFileByIoFile(file, true)
                if (vfsFile != null) {
                    vfsFiles.add(vfsFile)
                } else {
                    // if we want to undo file removal we need to create empty virtual file to write
                    // content
                    runInEdt {
                        WriteAction.run<Throwable> {
                            val parent = VfsUtil.createDirectories(file.parent)
                            vfsFile = parent.createChildData(this, file.name)
                            vfsFiles.add(vfsFile!!)
                        }
                    }
                }
            } else {
                LOG.warn("File $path is not a part of a project")
            }
        }
    }

    override fun run(): HandlerResponse {
        for (vfsFile in vfsFiles) {
            val count = getOpsCount(vfsFile)
            if (count == 0) {
                continue
            }

            runInEdt {
                getEditorWrapper(vfsFile).use { wrapper ->
                    val undoManager = UndoManagerImpl.getInstance(project)
                    val editor = wrapper.getFileEditor()
                    WriteAction.run<Throwable> {
                        try {
                            before(vfsFile)
                            var i = 0
                            while (i++ < count) {
                                runManagerAction(undoManager, editor)
                            }
                            commitAndFlush(vfsFile.findDocument())
                        } finally {
                            after(vfsFile)
                        }
                    }
                }
            }
        }
        return RESPONSE_OK
    }

    open fun getOpsCount(vfsFile: VirtualFile): Int {
        return getCopilotUndoManager().getUndoCount(vfsFile)
    }

    open fun before(vfsFile: VirtualFile) {
        getCopilotUndoManager().undoStart(vfsFile)
    }

    open fun runManagerAction(undoManager: UndoManager, editor: FileEditor) {
        undoManager.undo(editor)
    }

    open fun after(vfsFile: VirtualFile) {
        getCopilotUndoManager().undoDone(vfsFile)
        LOG.info("$vfsFile undo performed")
    }
}
