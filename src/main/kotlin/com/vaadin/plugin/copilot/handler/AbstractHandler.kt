package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.vaadin.plugin.copilot.CopilotPluginUtil
import java.io.File
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

abstract class AbstractHandler(val project: Project) : Runnable {

    val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

    class FileEditorWrapper(private val fileEditor: FileEditor, private val project: Project, private val closable: Boolean): AutoCloseable {

        fun getFileEditor(): FileEditor {
            return fileEditor
        }

        override fun close() {
            if (closable) {
                FileEditorManager.getInstance(project).closeFile(fileEditor.file)
            }
        }

    }

    @Throws(IOException::class)
    fun isFileInsideProject(project: Project, file: File): Boolean {
        val path = getRealPath(file)
        return (path.startsWith(project.basePath))
    }

    @Throws(IOException::class)
    private fun getRealPath(file: File): Path {
        val path = file.toPath()
        return try {
            path.toRealPath()
        } catch (e: NoSuchFileException) {
            // As we allow creating new files, we check the directory instead
            path.parent.toRealPath().resolve(path.fileName)
        }
    }

    fun getEditorWrapper(vfsFile: VirtualFile): FileEditorWrapper {
        val manager = FileEditorManager.getInstance(project)
        val editors = manager.getEditors(vfsFile)
        if (editors.isEmpty()) {
            return FileEditorWrapper(manager.openFile(vfsFile, false).first(), project, true)
        }
        return FileEditorWrapper(editors.first(), project, false)
    }

    fun commitAndFlush(vfsDoc: Document?) {
        if (vfsDoc != null) {
            PsiDocumentManager.getInstance(project).commitDocument(vfsDoc)
            FileDocumentManager.getInstance().saveDocument(vfsDoc)
        }
    }

}
