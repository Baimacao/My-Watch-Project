package com.example.lddc.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TextUtilsTest {

    @Test
    fun `converts full width symbols to half width`() {
        assertEquals("(测试):!", TextUtils.unifySymbols("（测试）：！"))
    }

    @Test
    fun `collapses repeated whitespace`() {
        assertEquals("a b c", TextUtils.unifySymbols("  a   b \t c  "))
    }

    @Test
    fun `keeps normal text untouched`() {
        assertEquals("Hello World", TextUtils.unifySymbols("Hello World"))
    }

    @Test
    fun `handles empty input`() {
        assertEquals("", TextUtils.unifySymbols(""))
    }
}
