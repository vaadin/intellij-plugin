package com.vaadin.plugin.copilot

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import java.awt.Component
import java.lang.management.ManagementFactory
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class CopilotErrorHandler : ErrorReportSubmitter() {

    override fun getReportActionText(): String {
        return "Report on GitHub"
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val throwableText = events.iterator().next().throwableText
        val appName = ApplicationInfo.getInstance().fullApplicationName
        val runtimeMXBean = ManagementFactory.getRuntimeMXBean()

        var body =
            "Plugin version: **${pluginDescriptor.version}**\n" +
                "IDE version: **$appName**\n" +
                "VM: **${runtimeMXBean.vmName + " " + runtimeMXBean.vmVersion + " " + runtimeMXBean.vmVendor}**\n" +
                "OS: **${System.getProperty("os.name") + " " + System.getProperty("os.version")}**\n\n"

        if (additionalInfo != null) {
            body += "Additional info:\n" + quote(additionalInfo) + "\n\n"
        }

        body += "Stacktrace:\n```\n${shortenStacktrace(throwableText)}\n```"

        BrowserUtil.browse(newIssueUri(issueTitle(throwableText), body))
        return true
    }

    internal companion object {

        private const val NEW_ISSUE_URL = "https://github.com/vaadin/intellij-plugin/issues/new"

        private const val TITLE_PREFIX = "[Error report] "

        /** GitHub cuts off longer titles, so shorten them here instead of being chopped mid-word. */
        private const val MAX_TITLE_LENGTH = 200

        /** Keeps the prefilled issue URL within what GitHub and the browser accept. */
        private const val MAX_STACKTRACE_LENGTH = 5000

        private const val ELISION = "\n\t... [middle of the stacktrace omitted] ...\n"

        internal fun issueTitle(throwableText: String): String {
            val firstLine = throwableText.lineSequence().first().trim()
            val maxMessageLength = MAX_TITLE_LENGTH - TITLE_PREFIX.length
            val message =
                if (firstLine.length <= maxMessageLength) firstLine
                else firstLine.take(maxMessageLength - 1).trimEnd() + "…"
            return TITLE_PREFIX + message
        }

        /**
         * Shortens an over-long stacktrace by dropping frames from the *middle*.
         *
         * The head holds the exception and the frames closest to it, while the tail holds the last `Caused by`, which
         * is where the plugin frames usually are. Keeping only the head drops exactly the part a report has to be
         * diagnosed from.
         */
        internal fun shortenStacktrace(throwableText: String): String {
            if (throwableText.length <= MAX_STACKTRACE_LENGTH) {
                return throwableText
            }

            val budget = MAX_STACKTRACE_LENGTH - ELISION.length
            val headLength = budget / 2
            // Cut on line boundaries so that no half frame is reported.
            val head = throwableText.take(headLength).substringBeforeLast('\n')
            val tail = throwableText.takeLast(budget - headLength).substringAfter('\n')
            return head + ELISION + tail
        }

        /**
         * Builds the prefilled new-issue URI.
         *
         * The query parameters are percent-encoded exactly once here, and handed over as a [URI] rather than as a
         * string, because [BrowserUtil.browse] escapes the strings it is given and would encode them a second time.
         */
        internal fun newIssueUri(title: String, body: String): URI {
            return URI("$NEW_ISSUE_URL?title=${encode(title)}&body=${encode(body)}")
        }

        private fun encode(value: String): String {
            // URLEncoder writes a space as '+', which means a space only in a query string.
            return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
        }

        /** Quotes every line, so that multi-line additional info renders as one Markdown blockquote. */
        private fun quote(additionalInfo: String): String {
            return additionalInfo.lineSequence().joinToString("\n") { "> $it" }
        }
    }
}
