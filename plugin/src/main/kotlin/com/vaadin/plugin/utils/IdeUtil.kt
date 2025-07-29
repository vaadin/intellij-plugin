package com.vaadin.plugin.utils

import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.VcsShowConfirmationOption
import java.nio.file.Path

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
}
