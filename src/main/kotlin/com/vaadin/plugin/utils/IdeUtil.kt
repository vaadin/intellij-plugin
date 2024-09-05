package com.vaadin.plugin.utils

import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.VcsShowConfirmationOption
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl

object IdeUtil {

    private fun willVcsPopupBeShown(project: Project): Boolean {
        val confirmation = VcsConfiguration.StandardConfirmation.ADD
        val value = ProjectLevelVcsManagerImpl.getInstanceImpl(project).getConfirmation(confirmation).value
        return value == VcsShowConfirmationOption.Value.SHOW_CONFIRMATION
    }

    private fun willHotSwapPopupBeShown(): Boolean {
        return DebuggerSettings.getInstance().RUN_HOTSWAP_AFTER_COMPILE == DebuggerSettings.RUN_HOTSWAP_ASK
    }

    fun bringToFront(project: Project) {
        runInEdt {
            ProjectUtil.focusProjectWindow(project, true)
        }
    }

    fun willBlockingPopupBeShown(project: Project, isJavaFile: Boolean): Boolean {
        if (willVcsPopupBeShown(project)) {
            return true
        }

        if (isJavaFile && willHotSwapPopupBeShown()) {
            return true
        }

        return false
    }

}
