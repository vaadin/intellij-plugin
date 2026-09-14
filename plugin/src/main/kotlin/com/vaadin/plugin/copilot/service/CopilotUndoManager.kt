package com.vaadin.plugin.copilot.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Stack

/**
 * Keeps track of which IDE undoable commands belong to a Copilot write, so that Copilot undo and redo revert exactly
 * those commands.
 *
 * A single write usually produces more than one command on the IDE undo stack: the write itself, plus whatever the IDE
 * does as a consequence of saving the file (reformat, optimize imports, ...). Those follow-up actions run
 * asynchronously, so the plugin cannot execute them inside its own command. Instead, every write records the time it
 * started, and undo reverts every command that is not older than that - see
 * [com.vaadin.plugin.copilot.handler.UndoHandler].
 */
@Service(Service.Level.PROJECT)
class CopilotUndoManager(val project: Project) {

    /**
     * One Copilot write together with everything the IDE did as a consequence of it.
     *
     * @param nanoTime [System.nanoTime] taken immediately before the write command is executed. It is compared against
     *   [com.intellij.openapi.command.undo.UndoManager.getNextUndoNanoTime], which tells when the command on top of the
     *   IDE undo stack was created.
     */
    class Batch(val nanoTime: Long) {

        /** How many commands this batch reverted, replayed as-is when it is redone. */
        var commandCount: Int = 0
    }

    private val undoStack: MutableMap<String, Stack<Batch>> = mutableMapOf()

    private val redoStack: MutableMap<String, Stack<Batch>> = mutableMapOf()

    fun fileWritten(file: VirtualFile, nanoTime: Long) {
        undoStack.getOrPut(file.path) { Stack() }.push(Batch(nanoTime))
        // a new write pushes new commands onto the IDE undo stack, which drops its redo history
        redoStack.remove(file.path)
    }

    fun peekUndoBatch(file: VirtualFile): Batch? = peek(file, undoStack)

    fun peekRedoBatch(file: VirtualFile): Batch? = peek(file, redoStack)

    /** Records that the pending undo batch reverted [commandCount] commands. */
    fun undoPerformed(file: VirtualFile, commandCount: Int) {
        move(file, undoStack, redoStack)?.commandCount = commandCount
    }

    fun redoPerformed(file: VirtualFile) {
        move(file, redoStack, undoStack)
    }

    /**
     * Drops the recorded undo history of a file, called when the pending batch could not be reverted. That means the
     * IDE no longer holds the commands of that write, in which case the older batches underneath cannot be reverted
     * either.
     */
    fun undoUnavailable(file: VirtualFile) {
        undoStack.remove(file.path)
    }

    fun redoUnavailable(file: VirtualFile) {
        redoStack.remove(file.path)
    }

    private fun peek(file: VirtualFile, stacks: MutableMap<String, Stack<Batch>>): Batch? {
        return stacks[file.path]?.takeIf { it.isNotEmpty() }?.peek()
    }

    private fun move(
        file: VirtualFile,
        fromStacksMap: MutableMap<String, Stack<Batch>>,
        targetStacksMap: MutableMap<String, Stack<Batch>>
    ): Batch? {
        val batch = peek(file, fromStacksMap) ?: return null
        fromStacksMap[file.path]!!.pop()
        if (fromStacksMap[file.path]!!.isEmpty()) {
            fromStacksMap.remove(file.path)
        }
        targetStacksMap.getOrPut(file.path) { Stack() }.push(batch)
        return batch
    }
}
