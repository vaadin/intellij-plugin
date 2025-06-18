package com.vaadin.plugin.actions

import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.vaadin.plugin.copilot.CopilotPluginUtil.Companion.NOTIFICATION_GROUP
import com.vaadin.plugin.utils.JetbrainsRuntimeUtil
import com.vaadin.plugin.utils.VaadinIcons
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class InstallJetbrainsRuntimeAction : AnAction() {

    internal class RevealJBRFileAction(val path: Path) : RevealFileAction() {
        override fun actionPerformed(e: AnActionEvent) {
            openFile(path)
        }
    }

    override fun actionPerformed(event: AnActionEvent) {

        if (event.project != null && event.project?.isDisposed == false) {
            JetbrainsRuntimeUtil.downloadLatestJBR(event.project!!).thenApply { afterDownload(it, event.project!!) }
        }
    }

    private fun afterDownload(result: JetbrainsRuntimeUtil.JBRInstallResult, project: Project) {
        if (result.status == JetbrainsRuntimeUtil.JBRInstallStatus.INSTALLED) {
            val version = result.path!!.nameWithoutExtension
            Notifications.Bus.notify(
                Notification(
                        NOTIFICATION_GROUP,
                        "JetBrains Runtime $version installed successfully",
                        NotificationType.INFORMATION)
                    .setIcon(VaadinIcons.VAADIN)
                    .addAction(RevealJBRFileAction(result.path)),
                project,
            )
            setupProjectSdk(project, result.path)
            return
        }

        if (result.status == JetbrainsRuntimeUtil.JBRInstallStatus.ALREADY_EXISTS) {
            val version = result.path!!.nameWithoutExtension
            Notifications.Bus.notify(
                Notification(
                        NOTIFICATION_GROUP,
                        "Latest JetBrains Runtime $version is already installed",
                        NotificationType.INFORMATION)
                    .setIcon(VaadinIcons.VAADIN)
                    .addAction(RevealJBRFileAction(result.path)),
                project,
            )
            setupProjectSdk(project, result.path)
            return
        }

        Notifications.Bus.notify(
            Notification(
                    NOTIFICATION_GROUP,
                    "JetBrains Runtime installation failed, see logs for details",
                    NotificationType.WARNING)
                .setIcon(VaadinIcons.VAADIN),
            project,
        )
    }

    private fun setupProjectSdk(project: Project, sdkHomePath: Path) {
        JetbrainsRuntimeUtil.addAndSetProjectSdk(project, sdkHomePath.toString())
    }
}
