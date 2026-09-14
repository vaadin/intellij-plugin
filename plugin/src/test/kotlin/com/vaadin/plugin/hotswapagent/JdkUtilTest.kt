package com.vaadin.plugin.hotswapagent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
