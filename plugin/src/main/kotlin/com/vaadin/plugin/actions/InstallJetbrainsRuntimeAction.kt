package com.vaadin.plugin.actions

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.vaadin.plugin.utils.JetbrainsRuntimeUtil
import java.nio.file.Path

class InstallJetbrainsRuntimeAction : AnAction() {

    internal class RevealJBRFileAction(val path: Path) : RevealFileAction() {
        override fun actionPerformed(e: AnActionEvent) {
            openFile(path)
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        if (event.project != null && event.project?.isDisposed == false) {
            JetbrainsRuntimeUtil.downloadAndSetupLatestJBR(event.project!!)
        }
    }
}
