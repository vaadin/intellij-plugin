package com.vaadin.plugin.utils

import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.VcsShowConfirmationOption
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.isEmpty

object IdeUtil {

    private val IDEA_DIR = ".idea"

    fun willVcsPopupBeShown(project: Project): Boolean {
        val confirmation = VcsConfiguration.StandardConfirmation.ADD
        val vcsManager = ProjectLevelVcsManager.getInstance(project)
        vcsManager.allActiveVcss.forEach {
            val value = vcsManager.getStandardConfirmation(confirmation, it).value
            if (value == VcsShowConfirmationOption.Value.SHOW_CONFIRMATION) {
                return true
            }
        }
        return false
    }

    fun willHotSwapPopupBeShown(): Boolean {
        return DebuggerSettings.getInstance().RUN_HOTSWAP_AFTER_COMPILE == DebuggerSettings.RUN_HOTSWAP_ASK
    }

    fun bringToFront(project: Project) {
        runInEdt { ProjectUtil.focusProjectWindow(project, true) }
    }

    fun getIdeaDirectoryPath(project: Project): Path? {
        return project.guessProjectDir()?.toNioPath()?.resolve(IDEA_DIR)
    }

    fun moveDirectoryContentsRobustly(sourceDir: File, targetDir: File) {
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            return
        }

        // Ensure the target directory exists
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val sourceFiles = sourceDir.listFiles() ?: return
        val failedMoves = mutableListOf<File>()

        // First pass: try to move all files and directories
        for (file in sourceFiles) {
            val targetFile = File(targetDir, file.name)

            try {
                if (file.isDirectory) {
                    // For directories, create target and recursively move contents
                    if (!targetFile.exists()) {
                        targetFile.mkdirs()
                    }
                    moveDirectoryContentsRobustly(file, targetFile)
                } else {
                    // For files, try to copy and then delete original
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }

                    // Use Files.move for atomic operation when possible
                    try {
                        Files.move(file.toPath(), targetFile.toPath())
                    } catch (e: Exception) {
                        // Fallback to copy and delete
                        Files.copy(file.toPath(), targetFile.toPath())
                        if (targetFile.exists() && targetFile.length() == file.length()) {
                            file.delete()
                        } else {
                            failedMoves.add(file)
                        }
                    }
                }
            } catch (e: Exception) {
                failedMoves.add(file)
            }
        }

        // Second pass: retry failed moves with different strategy
        for (file in failedMoves) {
            val targetFile = File(targetDir, file.name)
            try {
                if (file.isFile) {
                    // Force copy with FileUtil
                    com.intellij.openapi.util.io.FileUtil.copy(file, targetFile)
                    if (targetFile.exists() && targetFile.length() == file.length()) {
                        com.intellij.openapi.util.io.FileUtil.delete(file)
                    }
                }
            } catch (e: Exception) {
                // Log the failure but continue - at least we tried
                System.err.println("Failed to move file: ${file.absolutePath} to ${targetFile.absolutePath}: ${e.message}")
            }
        }

        // Final cleanup: try to delete the source directory if it's now empty
        try {
            val remainingFiles = sourceDir.listFiles()
            if (remainingFiles == null || remainingFiles.isEmpty()) {
                com.intellij.openapi.util.io.FileUtil.delete(sourceDir)
            } else {
                // Force delete any remaining files
                for (remainingFile in remainingFiles) {
                    try {
                        if (remainingFile.isFile) {
                            com.intellij.openapi.util.io.FileUtil.delete(remainingFile)
                        } else if (remainingFile.isDirectory) {
                            com.intellij.openapi.util.io.FileUtil.delete(remainingFile)
                        }
                    } catch (e: Exception) {
                        System.err.println("Could not delete remaining file: ${remainingFile.absolutePath}: ${e.message}")
                    }
                }
                // Try to delete the directory one more time
                FileUtil.delete(sourceDir)
            }
        } catch (e: Exception) {
            System.err.println("Could not delete source directory: ${sourceDir.absolutePath}: ${e.message}")
        }
    }
}
