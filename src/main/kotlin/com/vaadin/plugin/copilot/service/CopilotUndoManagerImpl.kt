package com.vaadin.plugin.copilot.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.util.Stack
import java.util.concurrent.locks.ReentrantLock

@Service(Service.Level.PROJECT)
class CopilotUndoManagerImpl(val project: Project) : CopilotUndoManager, Disposable {

    companion object {
        const val ACTIONS_ON_SAVE_WINDOW = 1000 // ms window for actions on save
    }

    class Batch() {
        private val time = System.currentTimeMillis()
        private var count = 0

        fun getCount(): Int {
            return count
        }

        fun increment() {
            count += 1
        }

        fun isInProgress(): Boolean {
            return System.currentTimeMillis() - time <= ACTIONS_ON_SAVE_WINDOW
        }
    }

    private val undoStack: MutableMap<String, Stack<Batch>> = mutableMapOf()
    private val redoStack: MutableMap<String, Stack<Batch>> = mutableMapOf()
    private val locks: MutableMap<String, ReentrantLock> = mutableMapOf()

    // increments latest batch for file if is current batch
    // locking prevents modifying stack during undo / redo
    private val bulkFileListener =
        object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                events
                    .filter { ev -> ev.isFromSave }
                    .filter { ev -> locks[ev.path] == null || !locks[ev.path]!!.isLocked }
                    .forEach {
                        val stack = undoStack[it.path]
                        if (stack != null) {
                            if (stack.peek().isInProgress()) {
                                stack.peek().increment()
                            } else {
                                undoStack.remove(it.path)
                            }
                        }
                    }
            }
        }

    init {
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, bulkFileListener)
    }

    override fun fileWritten(file: VirtualFile) {
        undoStack.getOrPut(file.path) { Stack() }.push(Batch())
    }

    override fun getUndoCount(file: VirtualFile): Int {
        return undoStack[file.path]?.peek()?.getCount() ?: 0
    }

    override fun getRedoCount(file: VirtualFile): Int {
        return redoStack[file.path]?.peek()?.getCount() ?: 0
    }

    override fun undoStart(file: VirtualFile) {
        locks.getOrPut(file.path) { ReentrantLock() }.lock()
    }

    override fun redoStart(file: VirtualFile) {
        locks.getOrPut(file.path) { ReentrantLock() }.lock()
    }

    override fun undoDone(file: VirtualFile) {
        popAndPush(file, undoStack, redoStack)
        locks[file.path]?.unlock()
    }

    override fun redoDone(file: VirtualFile) {
        popAndPush(file, redoStack, undoStack)
        locks[file.path]?.unlock()
    }

    private fun popAndPush(
        file: VirtualFile,
        fromStacksMap: MutableMap<String, Stack<Batch>>,
        targetStacksMap: MutableMap<String, Stack<Batch>>
    ) {
        val batch = fromStacksMap[file.path]?.pop()
        targetStacksMap.getOrPut(file.path) { Stack() }.push(batch)
        if (fromStacksMap[file.path]?.isEmpty() == true) {
            fromStacksMap.remove(file.path)
        }
    }

    override fun dispose() {
        undoStack.clear()
        redoStack.clear()
    }
}
