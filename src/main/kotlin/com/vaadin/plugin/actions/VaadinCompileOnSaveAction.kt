package com.vaadin.plugin.actions

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.ui.HotSwapUI
import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.ProjectTaskManager
import com.vaadin.plugin.copilot.CopilotPluginUtil

/** Action run after Document has been saved. Is not run for binary files. */
class VaadinCompileOnSaveAction : ActionsOnSaveFileDocumentManagerListener.ActionOnSave() {

    private val LOG: Logger = Logger.getInstance(CopilotPluginUtil::class.java)

    override fun isEnabledForProject(project: Project): Boolean {
        return VaadinCompileOnSaveActionInfo.isEnabledForProject(project)
    }

    override fun processDocuments(project: Project, documents: Array<Document?>) {
        if (documents.size != 1) {
            return
        }

        val doc = documents[0] ?: return
        val vfsFile = FileDocumentManager.getInstance().getFile(doc) ?: return
        compile(project, vfsFile)
    }

    private fun compile(project: Project, vfsFile: VirtualFile) {
        val fileIndex = ProjectFileIndex.getInstance(project)
        if (!fileIndex.isInSourceContent(vfsFile)) {
            // Something else than source file or resource
            return
        }

        // compile and reload Java files using HotSwap util
        if (vfsFile.extension.equals("java")) {
            val task =
                object : Task.Backgroundable(project, "Vaadin: compiling...") {
                    override fun run(indicator: ProgressIndicator) {
                        val session = DebuggerManagerEx.getInstanceEx(project).context.debuggerSession
                        if (session != null) {
                            LOG.info("${vfsFile.name} compiling...")
                            ReadAction.run<Exception> {
                                HotSwapUI.getInstance(project).compileAndReload(session, vfsFile)
                            }
                        }
                    }
                }
            ProgressManager.getInstance().run(task)
        } // process all other Documents to be included in output build directory
        else {
            ProjectTaskManager.getInstance(project).compile(vfsFile).then {
                if (it.hasErrors()) {
                    LOG.warn("Cannot process $vfsFile")
                }
            }
        }
    }
}
