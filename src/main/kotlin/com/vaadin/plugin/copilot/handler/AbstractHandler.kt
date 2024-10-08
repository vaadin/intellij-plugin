package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.psi.PsiDocumentManager
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.copilot.service.CopilotUndoManager
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.File

abstract class AbstractHandler(val project: Project) : Handler {

    val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

    val RESPONSE_OK = HandlerResponse(HttpResponseStatus.OK)

    val RESPONSE_BAD_REQUEST = HandlerResponse(HttpResponseStatus.BAD_REQUEST)

    val RESPONSE_ERROR = HandlerResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR)

    class FileEditorWrapper(
        private val fileEditor: FileEditor,
        private val project: Project,
        private val closable: Boolean,
    ) : AutoCloseable {

        fun getFileEditor(): FileEditor {
            return fileEditor
        }

        override fun close() {
            if (closable) {
                FileEditorManager.getInstance(project).closeFile(fileEditor.file)
            }
        }
    }

    fun isFileInsideProject(project: Project, file: File): Boolean {
        if (file.exists()) {
            val path = file.toPath()
            return path.toRealPath().startsWith(project.basePath!!)
        } else {
            // New file
            return isFileInsideProject(project, file.parentFile)
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

    open fun postSave(vfsFile: VirtualFile) {
        LOG.info("File $vfsFile contents saved")
        notifyUndoManager(vfsFile)
        commitAndFlush(vfsFile.findDocument())
        openFileInEditor(vfsFile)
    }

    private fun openFileInEditor(vfsFile: VirtualFile) {
        val openFileDescriptor = OpenFileDescriptor(project, vfsFile)
        FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, false)
    }

    private fun notifyUndoManager(vfsFile: VirtualFile) {
        getCopilotUndoManager().fileWritten(vfsFile)
    }

    fun commitAndFlush(vfsDoc: Document?) {
        if (vfsDoc != null) {
            PsiDocumentManager.getInstance(project).commitDocument(vfsDoc)
            FileDocumentManager.getInstance().saveDocuments(vfsDoc::equals)
        }
    }

    fun getCopilotUndoManager(): CopilotUndoManager {
        return project.getService(CopilotUndoManager::class.java)
    }
}
