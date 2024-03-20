package com.vaadin.plugin.copilot

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import java.awt.Component


class CopilotErrorHandler: ErrorReportSubmitter() {

    private val url = "https://github.com/vaadin/copilot-internal/issues/new"

    override fun getReportActionText(): String {
        return "Report on GitHub"
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {
        val throwableText = events.iterator().next().throwableText
        val firstLine = throwableText.split("\\n".toRegex(), 2)[0].trim()

        val appName = ApplicationInfo.getInstance().fullApplicationName
        val pluginVer = CopilotPluginUtil.getPluginVersion()

        val body = "Plugin version: **$pluginVer**\n" +
                "IDE version: **$appName**\n\n" +
                "Additional info:\n" +
                "> ${additionalInfo?.replace("\\n+".toRegex(), "\n")}" +
                "\n\n" +
                "Stacktrace:\n" +
                "```\n" +
                throwableText +
                "\n```"

        val title = "[Error report] $firstLine"
        BrowserUtil.browse("$url?title=$title&body=$body")
        return true
    }



}
