package com.vaadin.plugin.hotswapagent

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.concurrent.TimeUnit

/**
 * A [BasePlatformTestCase] rather than a JUnit 5 `@TestApplication` class: the JUnit 5 test application tears down
 * inside a fixed twenty second budget, which a slow CI agent misses. This base class reaches the same checks through
 * TestApplicationManager, which has no such deadline.
 */
class JdkUtilTest : BasePlatformTestCase() {

    // the SDK popup has to be opened from a thread that does not already hold the write-intent lock
    override fun runInDispatchThread(): Boolean = false

    fun testParseJavaVersionSupportsCommonFormats() {
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
            assertEquals("Failed for input '$input'", expected, JdkUtil.parseJavaVersion(input))
        }
    }

    fun testParseJavaVersionReturnsNullWhenNotParsable() {
        assertNull(JdkUtil.parseJavaVersion("abc"))
    }

    fun testSdkPopupIsBuiltAndShownWithWriteIntentReadAccess() {
        val application = ApplicationManager.getApplication()

        // Called from a thread without write-intent read access, like the Swing
        // callbacks the popup is opened from. Without that access the platform
        // refuses to read the SDK model while the popup is coming up.
        val hadWriteIntentReadAccess =
            application
                .executeOnPooledThread<Boolean> {
                    var acquired = false
                    JdkUtil.showSdkPopup(project, {}) { acquired = application.isWriteIntentLockAcquired }
                    acquired
                }
                .get(30, TimeUnit.SECONDS)

        assertTrue("SDK popup must be built and shown with write-intent read access", hadWriteIntentReadAccess)
    }
}
