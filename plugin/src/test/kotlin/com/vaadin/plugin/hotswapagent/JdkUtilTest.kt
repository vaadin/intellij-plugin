package com.vaadin.plugin.hotswapagent

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.junit5.TestApplication
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestApplication
class JdkUtilTest {

    @Test
    fun parseJavaVersionSupportsCommonFormats() {
        val cases =
            mapOf(
                "17" to 17,
                " 17 " to 17,
                "1.8" to 8,
                "1.8.0_292" to 8,
                "11.0.4" to 11,
                "21-ea" to 21,
            )

        cases.forEach { (input, expected) ->
            assertEquals(expected, JdkUtil.parseJavaVersion(input), "Failed for input '$input'")
        }
    }

    @Test
    fun parseJavaVersionReturnsNullWhenNotParsable() {
        assertNull(JdkUtil.parseJavaVersion("abc"))
    }

    @Test
    fun sdkModelAccessRunsWithWriteIntentReadAccess() {
        val application = ApplicationManager.getApplication()

        // A thread without write-intent read access, like the EDT when it dispatches a Swing event.
        val hadWriteIntentReadAccess =
            application
                .executeOnPooledThread<Boolean> {
                    var acquired = false
                    JdkUtil.withSdkModelAccess { acquired = application.isWriteIntentLockAcquired }
                    acquired
                }
                .get(30, TimeUnit.SECONDS)

        assertTrue(hadWriteIntentReadAccess, "SDK popup must be built and shown with write-intent read access")
    }
}
