package com.vaadin.plugin.copilot.service

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CopilotDotfileServiceImpl(private val project: Project) : CopilotDotfileService {

    private val DOTFILE = ".copilot-plugin"
    private val IDEA_DIR = ".idea"

    private val _fileExists = MutableStateFlow(false)
    private val fileExists: StateFlow<Boolean> = _fileExists.asStateFlow()

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    for (event in events) {
                        when (event) {
                            is VFileCreateEvent ->
                                if (Path.of(event.path).equals(getDotfile())) _fileExists.value = true
                            is VFileDeleteEvent ->
                                if (Path.of(event.path).equals(getDotfile())) _fileExists.value = false
                        }
                    }
                }
            })

        // Initial state
        _fileExists.value = getDotfile()?.let { Files.exists(it) } ?: false

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

    override fun getDotfileDirectory(): Path? {
        return project.guessProjectDir()?.toNioPath()?.resolve(IDEA_DIR)
    }

    override fun getDotfile(): Path? {
        return getDotfileDirectory()?.resolve(DOTFILE)
    }

    @Throws(IOException::class)
    override fun removeDotfile() {
        runInEdt { WriteAction.run<Throwable> { getDotfile()?.let { VfsUtil.findFile(it, false)?.delete(this) } } }
    }

    @Throws(IOException::class)
    override fun createDotfile(content: String) {
        runInEdt {
            WriteAction.run<Throwable> {
                val dotfileDirectory = getDotfileDirectory() ?: throw IOException("Could not find the .idea directory")
                val vfsDotfileDirectory =
                    VfsUtil.findFile(dotfileDirectory, true) ?: throw IOException("Could not find the .idea directory")
                val dotfile = vfsDotfileDirectory.createChildData(this, DOTFILE)
                VfsUtil.saveText(dotfile, content)
            }
        }
    }
}
