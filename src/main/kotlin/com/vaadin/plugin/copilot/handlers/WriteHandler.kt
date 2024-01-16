package com.vaadin.plugin.copilot.handlers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import com.vaadin.plugin.copilot.CopilotServer
import java.io.File
import java.io.IOException

class WriteHandler(project: Project, data: Map<String, Any>) : CopilotServer.CommandHandler, UndoableAction {

    private lateinit var originalContent: String
    private val content: String
    private val filePath: String

    init {
        content = data["content"] as String
        filePath = project.basePath + File.separator + data["file"]
        ApplicationManager.getApplication().executeOnPooledThread {
            runInEdt {
                val ioFile = File(filePath)
                val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
                originalContent = vfsFile?.readText().toString()
            }
        }
    }

    override fun handle() {
        redo()
    }

    override fun undo() {
        WriteAction.run<IOException> {
            val ioFile = File(filePath)
            val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
            if (vfsFile?.isWritable == true) {
                vfsFile.writeText(originalContent)
            }
        }
    }

    override fun redo() {
        WriteAction.run<IOException> {
            val ioFile = File(filePath)
            val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)
            if (vfsFile?.isWritable == true) {
                vfsFile.writeText(content)
            }
        }
    }

    override fun getAffectedDocuments(): Array<DocumentReference> {
        val ioFile = File(filePath)
        val vfsFile = VfsUtil.findFileByIoFile(ioFile, true)!!
        return arrayOf(DocumentReferenceManager.getInstance().create(vfsFile))
    }

    override fun isGlobal(): Boolean {
        return true
    }

}