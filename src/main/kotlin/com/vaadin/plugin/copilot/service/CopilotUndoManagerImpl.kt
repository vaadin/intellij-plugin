package com.vaadin.plugin.copilot.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.util.concurrent.locks.ReentrantLock

@Service(Service.Level.PROJECT)
class CopilotUndoManagerImpl(val project: Project) : CopilotUndoManager, Disposable {

    companion object {
        const val ACTIONS_ON_SAVE_WINDOW = 500 // ms window for actions on save
    }

    data class FileModification(val modified: Long, var count: Int) {
        fun isCurrent(): Boolean {
            return System.currentTimeMillis() - modified <= ACTIONS_ON_SAVE_WINDOW
        }
    }

    private val undoStack: MutableMap<String, FileModification> = mutableMapOf()
    private val redoStack: MutableMap<String, FileModification> = mutableMapOf()

    private val lock = ReentrantLock()

    private val bulkFileListener =
        object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                if (!lock.isLocked) {
                    events
                        .filter { ev -> ev.isFromSave }
                        .forEach {
                            if (undoStack.containsKey(it.path)) {
                                val fileModification = undoStack[it.path]!!
                                if (fileModification.isCurrent()) {
                                    undoStack[it.path]!!.count += 1
                                } else {
                                    undoStack.remove(it.path)
                                }
                            }
                        }
                }
            }
        }

    init {
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, bulkFileListener)
    }

    override fun fileWritten(file: VirtualFile) {
        undoStack[file.path] = FileModification(System.currentTimeMillis(), 0)
    }

    override fun getUndoCount(file: VirtualFile): Int {
        return undoStack[file.path]?.count ?: 0
    }

    override fun getRedoCount(file: VirtualFile): Int {
        return redoStack[file.path]?.count ?: 0
    }

    override fun undoStart(file: VirtualFile) {
        lock.lock()
    }

    override fun redoStart(file: VirtualFile) {
        lock.lock()
    }

    override fun undoDone(file: VirtualFile) {
        redoStack[file.path] = FileModification(System.currentTimeMillis(), undoStack[file.path]!!.count)
        undoStack.remove(file.path)
        lock.unlock()
    }

    override fun redoDone(file: VirtualFile) {
        undoStack[file.path] = FileModification(System.currentTimeMillis(), redoStack[file.path]!!.count)
        redoStack.remove(file.path)
        lock.unlock()
    }

    override fun dispose() {
        undoStack.clear()
        redoStack.clear()
    }
}
