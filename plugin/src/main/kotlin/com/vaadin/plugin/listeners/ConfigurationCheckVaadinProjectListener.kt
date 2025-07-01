package com.vaadin.plugin.listeners

import com.amplitude.ampli.ampli
import com.intellij.debugger.JavaDebuggerBundle
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.ide.actionsOnSave.ActionsOnSaveConfigurable
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.RunOnceUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import java.util.MissingResourceException
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.VcsShowConfirmationOption
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl
import com.vaadin.plugin.actions.VaadinCompileOnSaveActionInfo
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.copilot.service.CompilationStatusManagerService
import com.vaadin.plugin.copilot.service.CopilotUndoManager
import com.vaadin.plugin.utils.VaadinHomeUtil
import com.vaadin.plugin.utils.VaadinIcons
import com.vaadin.plugin.utils.trackPluginInitialized

class ConfigurationCheckVaadinProjectListener : VaadinProjectListener {

    companion object {
        const val NOTIFICATION_GROUP = "Vaadin configuration check"

        val VCS_CONFIRMATION_CONFIGURABLE =
            VcsBundle.message("configurable.VcsGeneralConfigurationConfigurable.display.name")
        val HOTSWAP_CONFIGURABLE = "HotSwap"

        val MESSAGE_RELOAD_CLASSES = JavaDebuggerBundle.message("label.debugger.hotswap.configurable.reload.classes")
        val MESSAGE_RELOAD_ALWAYS = JavaDebuggerBundle.message("label.debugger.hotswap.configurable.always")
        val MESSAGE_VCS = VcsBundle.message("version.control.main.configurable.name")
        val MESSAGE_ADD_SILENTLY = VcsBundle.message("radio.after.creation.add.silently")
        val MESSAGE_DO_NOT_ADD = VcsBundle.message("radio.after.creation.do.not.add")
        val MESSAGE_ASK = VcsBundle.message("radio.after.creation.show.options")
    }

    private var triggered = false

    override fun vaadinProjectDetected(project: Project) {
        if (!triggered) {
            triggered = true
            checkReloadClassesSetting(project)
            checkVcsAddConfirmationSetting(project)
            checkCompileOnSave(project)
            initAmplitude(project)
            initLocalServices(project)
            RunOnceUtil.runOnceForApp("hotswap-version-check-" + CopilotPluginUtil.getPluginVersion()) {
                VaadinHomeUtil.updateOrInstallHotSwapJar()
            }
        }
    }

    private fun initLocalServices(project: Project) {
        project.getService(CompilationStatusManagerService::class.java).subscribeToCompilationStatus()
        project.getService(CopilotUndoManager::class.java).subscribeToVfsChanges()
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
                "Change your $MESSAGE_VCS setting to \"$MESSAGE_DO_NOT_ADD\" or \"$MESSAGE_ADD_SILENTLY\". " +
                    "The current \"$MESSAGE_ASK\" setting may trigger blocking dialogs in the IDE while using Vaadin Copilot.",
                VCS_CONFIRMATION_CONFIGURABLE,
            )
        }
    }

    private fun checkCompileOnSave(project: Project) {
        if (!VaadinCompileOnSaveActionInfo.isEnabledForProject(project)) {
            notify(
                project,
                "Turn on Compile Java Files action while Debugging using HotSwap to see instant changes.",
                ActionsOnSaveConfigurable.CONFIGURABLE_ID,
            )
        }
    }

    private fun notify(project: Project, content: String, configurable: String) {
        val notificationId = "vaadin.notify.$configurable"
        if (PropertiesComponent.getInstance(project).getBoolean(notificationId, true)) {
            val notification =
                Notification(NOTIFICATION_GROUP, content, NotificationType.INFORMATION).setIcon(VaadinIcons.VAADIN)
            notification.addAction(createGoToConfigurationAction(project, configurable))
            notification.addAction(createDontAskAgainAction(project, notificationId))
            Notifications.Bus.notify(notification)
        }
    }

    private fun createGoToConfigurationAction(project: Project, configurable: String): AnAction {
        return NotificationAction.create("Go to settings...") { _, notification ->
            notification.hideBalloon()
            try {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, configurable)
            } catch (e: MissingResourceException) {
                // Handle missing resource bundles for other plugins (e.g., Grazie) in certain locales
                // Log the error but still attempt to open the settings dialog without the specific configurable
                CopilotPluginUtil.notify(
                    "Unable to open specific settings page due to missing localization resources. " +
                        "Opening general settings instead.",
                    NotificationType.WARNING,
                    project
                )
                ShowSettingsUtil.getInstance().showSettingsDialog(project)
            }
        }
    }

    private fun createDontAskAgainAction(project: Project, id: String): NotificationAction {
        return NotificationAction.create("Don't show again") { _, notification ->
            run {
                PropertiesComponent.getInstance(project).setValue(id, false, true)
                notification.expire()
            }
        }
    }

    private fun initAmplitude(project: Project) {
        trackPluginInitialized()
        ProjectManager.getInstance()
            .addProjectManagerListener(
                project,
                object : ProjectManagerListener {
                    override fun projectClosing(project: Project) {
                        ampli.flush()
                    }
                },
            )
    }
}
