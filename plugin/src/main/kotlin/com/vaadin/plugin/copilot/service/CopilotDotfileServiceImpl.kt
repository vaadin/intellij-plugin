package com.vaadin.plugin.copilot.service

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.vaadin.plugin.utils.IdeUtil.getIdeaDirectoryPath
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CopilotDotfileServiceImpl(private val project: Project) : CopilotDotfileService {

    private val DOTFILE = ".copilot-plugin"

    private val _fileExists = MutableStateFlow(false)
    private val fileExists: StateFlow<Boolean> = _fileExists.asStateFlow()

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val dotfilePath = getDotfilePath() ?: return
                    for (event in events) {
                        if (event !is VFileCreateEvent && event !is VFileDeleteEvent) continue
                        // The dotfile is a local file, so ignore unrelated events early. This also
                        // skips paths from non-local file systems such as the Database VFS, whose
                        // names may contain characters that are illegal in file paths (see #542).
                        if (!event.path.endsWith(DOTFILE)) continue
                        val eventPath =
                            try {
                                Path.of(event.path)
                            } catch (e: InvalidPathException) {
                                continue
                            }
                        if (eventPath == dotfilePath) {
                            _fileExists.value = event is VFileCreateEvent
                        }
                    }
                }
            })

        // Initial state
        _fileExists.value = getDotfilePath()?.let { Files.exists(it) } ?: false

        ProjectManager.getInstance()
            .addProjectManagerListener(
                project,
                object : ProjectManagerListener {
                    override fun projectClosing(project: Project) {
                        removeDotfile()
                    }
                },
            )
    }

    override fun isActive(): Boolean {
        return fileExists.value
    }

    override fun getDotfilePath(): Path? {
        return getIdeaDirectoryPath(project)?.resolve(DOTFILE)
    }

    @Throws(IOException::class)
    override fun removeDotfile() {
        runInEdt { WriteAction.run<Throwable> { getDotfilePath()?.let { VfsUtil.findFile(it, false)?.delete(this) } } }
    }

    @Throws(IOException::class)
    override fun createDotfile(content: String) {
        runInEdt {
            WriteAction.run<Throwable> {
                val dotfileDirectory =
                    getIdeaDirectoryPath(project) ?: throw IOException("Could not find the .idea directory")
                val vfsDotfileDirectory =
                    VfsUtil.createDirectoryIfMissing(dotfileDirectory.toString())
                        ?: throw IOException("Could not create .idea directory")
                val dotfile = vfsDotfileDirectory.createChildData(this, DOTFILE)
                VfsUtil.saveText(dotfile, content)
            }
        }
    }
}
