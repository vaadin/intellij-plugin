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

    override fun processDocuments(project: Project, documents: Array<Document>) {
        val task =
            object : Task.Backgroundable(project, "Vaadin: compiling...") {
                override fun run(indicator: ProgressIndicator) {

                    LOG.info("Processing ${documents.size} document(s)")

                    val fileIndex = ProjectFileIndex.getInstance(project)
                    val vfsFiles =
                        ReadAction.compute<List<VirtualFile>, Exception> {
                            documents
                                .mapNotNull { FileDocumentManager.getInstance().getFile(it) }
                                .filter { fileIndex.isInSourceContent(it) }
                        }

                    if (vfsFiles.isEmpty()) {
                        return
                    }

                    val javaFiles = vfsFiles.filter { it.extension == "java" }
                    if (javaFiles.isNotEmpty()) {
                        val session = DebuggerManagerEx.getInstanceEx(project).context.debuggerSession
                        if (session != null) {
                            ReadAction.run<Throwable> {
                                Thread.sleep(1)
                                HotSwapUI.getInstance(project).compileAndReload(session, *javaFiles.toTypedArray())
                            }
                        }
                        return
                    }

                    val nonJavaFiles = vfsFiles.filter { it.extension != "java" }
                    if (nonJavaFiles.isNotEmpty()) {
                        ProjectTaskManager.getInstance(project).compile(*nonJavaFiles.toTypedArray())
                    }
                }
            }
        ProgressManager.getInstance().run(task)
    }
}
