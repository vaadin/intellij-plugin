package com.vaadin.plugin.copilot

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CopilotErrorHandlerTest {

    private val header =
        "com.intellij.openapi.diagnostic.UnhandledException: Access is allowed from write thread only; " +
            "If you access or modify model on EDT consider wrapping your code in WriteIntentReadAction or " +
            "ReadAction; see https://jb.gg/ij-platform-threading for details"

    private val pluginFrame =
        "\tat com.vaadin.plugin.ui.VaadinStatusBarInfoPopupPanel.createJbrDownloadButton\$lambda\$3" +
            "(VaadinStatusBarInfoPopupPanel.kt:110)"

    /** A report as long as the ones actually submitted, where the plugin frame is in the last `Caused by`. */
    private fun reportedStacktrace(): String {
        val platformFrames = (1..200).map { "\tat com.intellij.ide.IdeEventQueue.dispatchEvent(IdeEventQueue.kt:$it)" }
        val causeFrames = (1..200).map { "\tat com.intellij.ui.popup.AbstractPopup.show(AbstractPopup.java:$it)" }
        return (listOf(header) +
                platformFrames +
                listOf("Caused by: com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments: $header") +
                causeFrames +
                listOf(pluginFrame))
            .joinToString("\n")
    }

    @Test
    fun shortenedStacktraceKeepsTheRootCauseWithThePluginFrames() {
        val shortened = CopilotErrorHandler.shortenStacktrace(reportedStacktrace())

        assertTrue(shortened.length <= 5000, "Stacktrace must be shortened, was ${shortened.length} characters")
        assertTrue(shortened.startsWith(header), "The exception itself must be reported")
        // Cutting the tail instead of the middle is what made the submitted reports impossible to
        // diagnose.
        assertTrue(shortened.contains(pluginFrame), "The root cause with the plugin frames must be reported")
        assertTrue(shortened.contains("omitted"), "Dropped frames must be marked as such")
    }

    @Test
    fun stacktraceThatFitsIsReportedAsIs() {
        val stacktrace = "$header\n$pluginFrame"

        assertEquals(stacktrace, CopilotErrorHandler.shortenStacktrace(stacktrace))
    }

    @Test
    fun titleIsShortenedToWhatGitHubKeeps() {
        val title = CopilotErrorHandler.issueTitle(reportedStacktrace())

        assertTrue(title.length <= 200, "Title must fit in an issue title, was ${title.length} characters")
        assertTrue(title.startsWith("[Error report] com.intellij.openapi.diagnostic.UnhandledException:"), title)
        assertTrue(title.endsWith("…"), "A shortened title must be marked as such, was '$title'")
    }

    @Test
    fun titleThatFitsIsReportedAsIs() {
        val stacktrace = "java.lang.NullPointerException: boom\n$pluginFrame"

        assertEquals("[Error report] java.lang.NullPointerException: boom", CopilotErrorHandler.issueTitle(stacktrace))
    }

    @Test
    fun issueUriEncodesParametersExactlyOnce() {
        val title = "[Error report] Access is allowed from write thread only"
        // Characters that have to survive: they either delimit the query or are not allowed in a
        // URI at all.
        val body = "Stacktrace:\n```\n\tat Foo.bar(100% & a=1)\n```"

        val uri = CopilotErrorHandler.newIssueUri(title, body)
        val parameters =
            uri.rawQuery.split("&").associate {
                it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), StandardCharsets.UTF_8)
            }

        assertEquals("https://github.com/vaadin/intellij-plugin/issues/new", uri.toString().substringBefore('?'))
        // Decoding once has to give back the report. Encoding it twice, as browsing a string does,
        // would prefill
        // the issue with the escapes themselves ('%20', '%0A', ...) instead.
        assertEquals(title, parameters["title"])
        assertEquals(body, parameters["body"])
    }
}
