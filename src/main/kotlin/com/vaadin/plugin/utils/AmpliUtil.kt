package com.vaadin.plugin.utils

import com.amplitude.ampli.Ampli
import com.amplitude.ampli.EventOptions
import com.amplitude.ampli.LoadOptions
import com.amplitude.ampli.ProjectCreated
import com.amplitude.ampli.ampli
import com.intellij.internal.statistic.DeviceIdManager
import com.intellij.internal.statistic.DeviceIdManager.DeviceIdToken
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.util.io.DigestUtil
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.ui.settings.VaadinSettings
import com.vaadin.pro.licensechecker.LocalProKey
import com.vaadin.pro.licensechecker.ProKey
import java.nio.charset.Charset

private val eventOptions =
    EventOptions(
        versionName = CopilotPluginUtil.getPluginVersion(),
        platform = "intellij",
        deviceModel = if (isUltimate()) "ultimate" else "community",
        language = System.getProperty("user.language"),
        country = System.getProperty("user.country"),
        region = System.getProperty("user.region"),
        osName = System.getProperty("os.name"),
        osVersion = System.getProperty("os.version"),
        appVersion = ApplicationInfo.getInstance().fullVersion)

private var userId: String? = null

private fun getUserId(): String? {
    if (userId == null) {
        val proKey: ProKey? = LocalProKey.get()
        userId =
            if (proKey != null) {
                "pro-${DigestUtil.sha256Hex(proKey.proKey.toByteArray(Charset.defaultCharset()))}"
            } else {
                DeviceIdManager.getOrGenerateId(object : DeviceIdToken {}, "vaadin-plugin")
            }
        ampli.load(LoadOptions(Ampli.Environment.IDEPLUGINS))
        ampli.identify(userId, eventOptions)
    }
    return userId
}

private val enabled: Boolean
    get() = VaadinSettings.instance.state.sendUsageStatistics

internal fun trackPluginInitialized() {
    if (enabled) {
        ampli.pluginInitialized(getUserId())
    }
}

internal fun trackProjectCreated(downloadUrl: String) {
    if (enabled) {
        ampli.projectCreated(getUserId(), ProjectCreated(downloadUrl))
    }
}

internal fun trackManualCopilotRestart() {
    if (enabled) {
        ampli.manualCopilotRestart(getUserId())
    }
}
