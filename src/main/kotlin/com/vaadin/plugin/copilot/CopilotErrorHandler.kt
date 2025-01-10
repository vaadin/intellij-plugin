package com.vaadin.plugin.copilot

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import java.awt.Component
import java.lang.management.ManagementFactory

class CopilotErrorHandler : ErrorReportSubmitter() {

    private val ghMaxBodyLength = 5000

    private val url = "https://github.com/vaadin/intellij-plugin/issues/new"

    override fun getReportActionText(): String {
        return "Report on GitHub"
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        var throwableText = events.iterator().next().throwableText
        val firstLine = throwableText.split("\\n".toRegex(), 2)[0].trim()
        val appName = ApplicationInfo.getInstance().fullApplicationName

        if (throwableText.length > ghMaxBodyLength) {
            throwableText = throwableText.substring(0, ghMaxBodyLength)
        }

        val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
        var body =
            "Plugin version: **${pluginDescriptor.version}**\n" +
                "IDE version: **$appName**\n" +
                "VM: **${runtimeMXBean.vmName + " " + runtimeMXBean.vmVersion + " " + runtimeMXBean.vmVendor}**\n" +
                "OS: **${System.getProperty("os.name") + " " + System.getProperty("os.version")}**\n\n"

        if (additionalInfo != null) {
            body += "Additional info:\n" + "> ${additionalInfo.replace("\\n+".toRegex(), "\n")}" + "\n\n"
        }

        body += "Stacktrace:\n```\n$throwableText\n```"

        val title = "[Error report] $firstLine"
        BrowserUtil.browse("$url?title=$title&body=$body")
        return true
    }
}
