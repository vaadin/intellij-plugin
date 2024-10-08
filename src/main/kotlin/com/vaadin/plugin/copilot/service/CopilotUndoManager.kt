package com.vaadin.plugin.copilot.service

import com.intellij.openapi.vfs.VirtualFile

interface CopilotUndoManager {

    fun fileWritten(file: VirtualFile)

    fun getUndoCount(file: VirtualFile): Int

    fun getRedoCount(file: VirtualFile): Int

    fun undoStart(file: VirtualFile)

    fun undoDone(file: VirtualFile)

    fun redoStart(file: VirtualFile)

    fun redoDone(file: VirtualFile)
}
