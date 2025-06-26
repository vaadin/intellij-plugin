package com.vaadin.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.vaadin.plugin.utils.JetbrainsRuntimeUtil

class InstallJetbrainsRuntimeAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        if (event.project != null && event.project?.isDisposed == false) {
            JetbrainsRuntimeUtil.downloadAndSetupLatestJBR(event.project!!)
        }
    }
}
