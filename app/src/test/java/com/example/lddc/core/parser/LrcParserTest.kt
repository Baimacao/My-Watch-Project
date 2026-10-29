package com.example.lddc.core.parser

import com.example.lddc.common.models.enums.LyricsType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    private val parser = LrcParser()

    @Test
    fun `supports lrc and plain only`() {
        assertTrue(parser.supports("lrc"))
        assertTrue(parser.supports("LRC"))
        assertTrue(parser.supports("plain"))
        assertFalse(parser.supports("krc"))
        assertFalse(parser.supports("qrc"))
    }

    @Test
    fun `parses timestamps into milliseconds`() {
        val lines = parser.parse("[00:01.50]第一行\n[00:12.34]第二行")
        assertEquals(2, lines.size)
        assertEquals(1500, lines[0].start)
        assertEquals(12340, lines[1].start)
        assertEquals("第一行", lines[0].words.single().text)
    }

    @Test
    fun `sorts lines and fills end time from next line`() {
        val lines = parser.parse("[00:05.00]后\n[00:01.00]前")
        assertEquals(listOf(1000, 5000), lines.map { it.start })
        assertEquals(5000, lines[0].end)
        // 最后一行没有下一行，默认补 5 秒
        assertEquals(10000, lines[1].end)
    }

    @Test
    fun `reads metadata tags`() {
        val (tags, lines, type) = parser.parseWithTags(
            "[ti:测试歌曲]\n[ar:测试歌手]\n[al:测试专辑]\n[00:00.00]正文"
        )
        assertEquals("测试歌曲", tags["ti"])
        assertEquals("测试歌手", tags["ar"])
        assertEquals("测试专辑", tags["al"])
        assertEquals(1, lines.size)
        assertEquals(LyricsType.LINEBYLINE, type)
    }

    @Test
    fun `expands repeated timestamps into separate lines`() {
        val lines = parser.parse("[00:01.00][00:03.00]重复")
        assertEquals(2, lines.size)
        assertEquals(listOf(1000, 3000), lines.map { it.start })
        assertTrue(lines.all { it.words.single().text == "重复" })
    }

    @Test
    fun `ignores blank lines and junk`() {
        val lines = parser.parse("\n   \n随便一行没有时间戳\n[00:02.00]有效")
        assertEquals(1, lines.size)
        assertEquals(2000, lines[0].start)
    }

    @Test
    fun `empty content yields empty list`() {
        assertTrue(parser.parse("").isEmpty())
    }
}
