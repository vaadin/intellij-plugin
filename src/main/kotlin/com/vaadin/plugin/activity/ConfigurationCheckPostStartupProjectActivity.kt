package com.vaadin.plugin.activity

import com.intellij.debugger.JavaDebuggerBundle
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.ide.IdeCoreBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.VcsShowConfirmationOption
import com.intellij.openapi.vcs.configurable.VcsGeneralSettingsConfigurable
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl
import com.vaadin.plugin.copilot.CopilotPluginUtil

class ConfigurationCheckPostStartupProjectActivity : ProjectActivity {

    companion object {
        const val NOTIFICATION_GROUP = "Vaadin Configuration Check"
        val NOTIFICATION_ICON = IconLoader.getIcon("/icons/vaadin.svg", CopilotPluginUtil::class.java)

        val MESSAGE_RELOAD_CLASSES = JavaDebuggerBundle.message("label.debugger.hotswap.configurable.reload.classes")
        val MESSAGE_RELOAD_ALWAYS = JavaDebuggerBundle.message("label.debugger.hotswap.configurable.always")
        val MESSAGE_VCS_CONFIGURABLE = VcsBundle.message("version.control.main.configurable.name")
        val MESSAGE_WHEN_FILES_CREATED = VcsBundle.message("settings.border.when.files.are.created")
        val MESSAGE_ADD_SILENTLY = VcsBundle.message("radio.after.creation.add.silently")
    }

    override suspend fun execute(project: Project) {
        checkReloadClassesSetting(project)
        checkVcsAddConfirmationSetting(project)
    }

    private fun checkReloadClassesSetting(project: Project) {
        val dm = DebuggerSettings.getInstance()
        if (dm.RUN_HOTSWAP_AFTER_COMPILE != DebuggerSettings.RUN_HOTSWAP_ALWAYS) {
            val action = NotificationAction.create("Go to configuration...", com.intellij.util.Consumer {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "reference.idesettings.debugger.hotswap")
            })

            if (dm.RUN_HOTSWAP_AFTER_COMPILE == DebuggerSettings.RUN_HOTSWAP_ASK) {
                notify(
                    "Your HotSwap class reload setting require confirmation each time file is going to be reloaded. " +
                            "It will cause popups appear while working with Vaadin Copilot. " +
                            "To improve experience please change \"${MESSAGE_RELOAD_CLASSES}\" to \"${MESSAGE_RELOAD_ALWAYS}\"",
                    action
                )
            }

            if (dm.RUN_HOTSWAP_AFTER_COMPILE == DebuggerSettings.RUN_HOTSWAP_NEVER) {
                notify(
                    "Your HotSwap class reload is turned off. It will cause changes are not reflected in your application. " +
                            "To improve experience please change \"$MESSAGE_RELOAD_CLASSES\" to \"$MESSAGE_RELOAD_ALWAYS\"",
                    action
                )
            }
        }
    }

    private fun checkVcsAddConfirmationSetting(project: Project) {
        val confirmation = VcsConfiguration.StandardConfirmation.ADD
        val value = ProjectLevelVcsManagerImpl.getInstanceImpl(project).getConfirmation(confirmation).value

        if (value == VcsShowConfirmationOption.Value.SHOW_CONFIRMATION) {
            val action = NotificationAction.create("Go to configuration...", com.intellij.util.Consumer {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, VcsGeneralSettingsConfigurable::class.java)
            })

            notify(
                "Your $MESSAGE_VCS_CONFIGURABLE settings require confirmation after creating each new file. " +
                        "It will cause popups appear while working with Vaadin Copilot. " +
                        "To improve experience please change \"$MESSAGE_WHEN_FILES_CREATED\" to \"$MESSAGE_ADD_SILENTLY\"",
                action
            )
        }
    }

    private fun notify(content: String, action: AnAction) {
        val notification = Notification(
            NOTIFICATION_GROUP,
            content,
            NotificationType.INFORMATION
        ).setIcon(NOTIFICATION_ICON)
        notification.addAction(action)
        notification.addAction(createDontAskAgainAction())
        Notifications.Bus.notify(notification)
    }

    private fun createDontAskAgainAction(): NotificationAction {
        return NotificationAction.create(IdeCoreBundle.message("dialog.options.do.not.ask")) { _, notification ->
            notification.expire()
        }
    }

}
