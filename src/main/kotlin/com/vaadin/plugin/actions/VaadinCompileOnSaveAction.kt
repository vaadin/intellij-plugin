package com.vaadin.plugin.actions

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.ui.HotSwapUI
import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.vaadin.plugin.actions.VaadinCompileOnSaveActionInfo.Companion.DEFAULT
import com.vaadin.plugin.actions.VaadinCompileOnSaveActionInfo.Companion.PROPERTY
import com.vaadin.plugin.copilot.CopilotPluginUtil

class VaadinCompileOnSaveAction :
    ActionsOnSaveFileDocumentManagerListener.ActionOnSave() {

    private val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

    override fun isEnabledForProject(project: Project): Boolean {
        return PropertiesComponent.getInstance(project)
            .getBoolean(PROPERTY, DEFAULT)
    }

    override fun processDocuments(
        project: Project,
        documents: Array<Document?>
    ) {
        if (documents.size != 1) {
            return
        }

        val doc = documents[0] ?: return
        val vfsFile = FileDocumentManager.getInstance().getFile(doc) ?: return
        compile(project, vfsFile)
    }

    fun compile(project: Project, vfsFile: VirtualFile) {
        if (!vfsFile.extension.equals("java")) {
            return
        }

        val task =
            object : Task.Backgroundable(project, "Vaadin: compiling...") {
                override fun run(indicator: ProgressIndicator) {
                    val session =
                        DebuggerManagerEx.getInstanceEx(project)
                            .context
                            .debuggerSession
                    if (session != null) {
                        HotSwapUI.getInstance(project)
                            .compileAndReload(session, vfsFile)
                        LOG.info("File $vfsFile compiled")
                    }
                }
            }
        ProgressManager.getInstance().run(task)
    }
}
