package com.vaadin.plugin.listeners

import com.intellij.debugger.JavaDebuggerBundle
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.ide.IdeCoreBundle
import com.intellij.ide.util.RunOnceUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.VcsShowConfirmationOption
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.utils.VaadinHomeUtil
import com.vaadin.plugin.utils.VaadinIcons

class ConfigurationCheckVaadinProjectListener : VaadinProjectListener {

    companion object {
        const val NOTIFICATION_GROUP = "Vaadin Configuration Check"

        val VCS_CONFIRMATION_CONFIGURABLE =
            VcsBundle.message("configurable.VcsGeneralConfigurationConfigurable.display.name")
        val HOTSWAP_CONFIGURABLE = "HotSwap"

        val MESSAGE_RELOAD_CLASSES = JavaDebuggerBundle.message("label.debugger.hotswap.configurable.reload.classes")
        val MESSAGE_RELOAD_ALWAYS = JavaDebuggerBundle.message("label.debugger.hotswap.configurable.always")
        val MESSAGE_VCS = VcsBundle.message("version.control.main.configurable.name")
        val MESSAGE_WHEN_FILES_CREATED = VcsBundle.message("settings.border.when.files.are.created")
        val MESSAGE_ADD_SILENTLY = VcsBundle.message("radio.after.creation.add.silently")
        val MESSAGE_DO_NOT_ADD = VcsBundle.message("radio.after.creation.do.not.add")
        val MESSAGE_DONT_ASK_AGAIN = IdeCoreBundle.message("dialog.options.do.not.ask")
    }

    private var triggered = false

    override fun vaadinProjectDetected(project: Project) {
        if (!triggered) {
            triggered = true
            checkReloadClassesSetting(project)
            checkVcsAddConfirmationSetting(project)
            RunOnceUtil.runOnceForApp("hotswap-version-check-" + CopilotPluginUtil.getPluginVersion()) {
                VaadinHomeUtil.updateOrInstallHotSwapJar()
            }
        }
    }

    private fun checkReloadClassesSetting(project: Project) {
        val dm = DebuggerSettings.getInstance()
        if (dm.RUN_HOTSWAP_AFTER_COMPILE == DebuggerSettings.RUN_HOTSWAP_ASK) {
            notify(
                project,
                "Your $HOTSWAP_CONFIGURABLE class reload setting requires confirmation each time file is going to be reloaded. " +
                    "It will cause popups appear while working with Vaadin Copilot. " +
                    "To improve experience please set \"$MESSAGE_RELOAD_CLASSES\" to \"$MESSAGE_RELOAD_ALWAYS\"",
                HOTSWAP_CONFIGURABLE,
            )
        }

        if (dm.RUN_HOTSWAP_AFTER_COMPILE == DebuggerSettings.RUN_HOTSWAP_NEVER) {
            notify(
                project,
                "Your $HOTSWAP_CONFIGURABLE class reload is turned off. It will cause changes are not reflected in your application. " +
                    "To improve experience please set \"$MESSAGE_RELOAD_CLASSES\" to \"$MESSAGE_RELOAD_ALWAYS\"",
                HOTSWAP_CONFIGURABLE,
            )
        }
    }

    private fun checkVcsAddConfirmationSetting(project: Project) {
        val confirmation = VcsConfiguration.StandardConfirmation.ADD
        val value = ProjectLevelVcsManagerImpl.getInstanceImpl(project).getConfirmation(confirmation).value
        if (value == VcsShowConfirmationOption.Value.SHOW_CONFIRMATION) {
            notify(
                project,
                "Your $MESSAGE_VCS settings requires confirmation after creating each new file. " +
                    "It will cause popups appear while working with Vaadin Copilot. " +
                    "To improve experience please set \"$MESSAGE_WHEN_FILES_CREATED\" to \"$MESSAGE_ADD_SILENTLY\" or \"$MESSAGE_DO_NOT_ADD\"",
                VCS_CONFIRMATION_CONFIGURABLE,
            )
        }
    }

    private fun notify(project: Project, content: String, configurable: String) {
        val notification =
            Notification(NOTIFICATION_GROUP, content, NotificationType.INFORMATION).setIcon(VaadinIcons.VAADIN)
        notification.addAction(createGoToConfigurationAction(project, configurable))
        notification.addAction(createDontAskAgainAction())
        Notifications.Bus.notify(notification)
    }

    private fun createGoToConfigurationAction(project: Project, configurable: String): AnAction {
        return NotificationAction.create("Show in settings...") { _, notification ->
            notification.hideBalloon()
            ShowSettingsUtil.getInstance().showSettingsDialog(project, configurable)
        }
    }

    private fun createDontAskAgainAction(): NotificationAction {
        return NotificationAction.create(MESSAGE_DONT_ASK_AGAIN) { _, notification -> notification.expire() }
    }
}