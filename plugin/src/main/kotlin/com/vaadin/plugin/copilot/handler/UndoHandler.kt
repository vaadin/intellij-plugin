package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.vaadin.plugin.copilot.service.CopilotUndoManager.Batch
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.File

open class UndoHandler(project: Project, data: Map<String, Any>) : AbstractHandler(project) {

    companion object {
        /** Safety net against a command that refuses to be reverted, which would loop forever. */
        const val MAX_COMMANDS_PER_BATCH = 100
    }

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
        var performed = false
        for (vfsFile in vfsFiles) {
            val batch = getBatch(vfsFile) ?: continue

            performed = true

            runInEdt {
                getEditorWrapper(vfsFile).use { wrapper ->
                    val undoManager = UndoManager.getInstance(project)
                    val editor = wrapper.getFileEditor()
                    WriteAction.run<Throwable> {
                        val commandCount = runManagerActions(undoManager, editor, batch)
                        if (commandCount > 0) {
                            commitAndFlush(vfsFile.findDocument())
                            actionsPerformed(vfsFile, commandCount)
                        } else {
                            LOG.info("$vfsFile is no longer in the IDE undo history")
                            actionsUnavailable(vfsFile)
                        }
                    }
                }
            }
        }

        val data = mapOf("performed" to performed)
        return HandlerResponse(HttpResponseStatus.OK, data)
    }

    open fun getBatch(vfsFile: VirtualFile): Batch? {
        return getCopilotUndoManager().peekUndoBatch(vfsFile)
    }

    /**
     * Reverts every command the IDE created at or after the Copilot write this [batch] stands for, and stops at the
     * first command that predates it. The platform timestamps every undoable command, so the follow-up actions
     * triggered by the write are covered no matter how long they took to arrive.
     */
    open fun runManagerActions(undoManager: UndoManager, editor: FileEditor, batch: Batch): Int {
        var commandCount = 0
        while (commandCount < MAX_COMMANDS_PER_BATCH &&
            undoManager.isUndoAvailable(editor) &&
            undoManager.getNextUndoNanoTime(editor) >= batch.nanoTime) {
            undoManager.undo(editor)
            commandCount++
        }
        return commandCount
    }

    open fun actionsPerformed(vfsFile: VirtualFile, commandCount: Int) {
        getCopilotUndoManager().undoPerformed(vfsFile, commandCount)
        LOG.info("$vfsFile undo performed, $commandCount command(s) reverted")
    }

    open fun actionsUnavailable(vfsFile: VirtualFile) {
        getCopilotUndoManager().undoUnavailable(vfsFile)
    }
}
