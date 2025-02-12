package com.vaadin.plugin.utils

import ai.grazie.utils.mpp.UUID
import com.amplitude.ampli.Ampli
import com.amplitude.ampli.EventOptions
import com.amplitude.ampli.LoadOptions
import com.amplitude.ampli.ManualCopilotRestart
import com.amplitude.ampli.PluginInitialized
import com.amplitude.ampli.ProjectCreated
import com.amplitude.ampli.ampli
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.util.io.DigestUtil
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.ui.settings.VaadinSettings
import com.vaadin.pro.licensechecker.LocalProKey
import elemental.json.Json
import java.io.IOException
import java.nio.charset.Charset

private val eventOptions =
    EventOptions(
        platform = ApplicationInfo.getInstance().fullApplicationName,
        deviceModel = if (isUltimate()) "ultimate" else "community",
        language = System.getProperty("user.language"),
        country = System.getProperty("user.country"),
        region = System.getProperty("user.region"),
        osName = System.getProperty("os.name"),
        osVersion = System.getProperty("os.version"),
        appVersion = CopilotPluginUtil.getPluginVersion())

private var userId: String? = null

private var vaadiner: Boolean? = null

private fun getUserId(): String {
    if (userId == null) {
        userId =
            try {
                VaadinHomeUtil.getUserKey()
            } catch (e: IOException) {
                "user-" + UUID.random().text
            }
        ampli.load(LoadOptions(Ampli.Environment.IDEPLUGINS))
        ampli.identify(userId, eventOptions)
    }
    return userId!!
}

private fun isVaadiner(): Boolean {
    if (vaadiner == null) {
        val proKey = LocalProKey.get()
        if (proKey != null) {
            val json = Json.parse(proKey.toJson())
            vaadiner = if (json.hasKey("username")) json.getString("username").endsWith("@vaadin.com") else false
        } else {
            vaadiner = false
        }
    }
    return vaadiner!!
}

private fun getProKeyDigest(): String? {
    val proKey = LocalProKey.get()
    return if (proKey != null) {
        DigestUtil.sha256Hex(proKey.proKey.toByteArray(Charset.defaultCharset()))
    } else {
        null
    }
}

private val enabled: Boolean
    get() = VaadinSettings.instance.state.sendUsageStatistics

internal fun trackPluginInitialized() {
    if (enabled) {
        ampli.pluginInitialized(getUserId(), PluginInitialized(isVaadiner(), getProKeyDigest()))
    }
}

internal fun trackProjectCreated(downloadUrl: String) {
    if (enabled) {
        ampli.projectCreated(getUserId(), ProjectCreated(isVaadiner(), downloadUrl))
    }
}

internal fun trackManualCopilotRestart() {
    if (enabled) {
        ampli.manualCopilotRestart(getUserId(), ManualCopilotRestart(isVaadiner()))
    }
}
