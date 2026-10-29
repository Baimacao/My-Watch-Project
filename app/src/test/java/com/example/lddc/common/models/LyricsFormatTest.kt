package com.example.lddc.common.models

import com.example.lddc.common.models.enums.LyricsFormat
import com.example.lddc.common.models.enums.Source
import com.example.lddc.common.models.info.LyricInfo
import com.example.lddc.common.models.info.SongInfo
import com.example.lddc.common.models.lyrics.Lyrics
import com.example.lddc.common.models.lyrics.LyricsLine
import com.example.lddc.common.models.lyrics.LyricsWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFormatTest {

    private fun sampleLyrics(): Lyrics {
        val info = LyricInfo(
            source = Source.QM,
            songinfo = SongInfo(source = Source.QM, id = "1", title = "测试歌曲")
        )
        val lines = listOf(
            LyricsLine(
                start = 1000,
                end = 3000,
                words = listOf(
                    LyricsWord(start = 1000, end = 2000, text = "你"),
                    LyricsWord(start = 2000, end = 3000, text = "好")
                )
            ),
            LyricsLine(
                start = 4000,
                end = 6000,
                words = listOf(LyricsWord(start = 4000, end = 6000, text = "世界"))
            )
        )
        return Lyrics(
            info = info,
            tags = mapOf("ti" to "测试歌曲", "ar" to "测试歌手"),
            data = mapOf("orig" to lines)
        )
    }

    @Test
    fun `exposes metadata from info and tags`() {
        val lyrics = sampleLyrics()
        assertEquals("测试歌曲", lyrics.title)
        assertEquals("测试歌手", lyrics.artist?.toString())
        assertEquals(Source.QM, lyrics.source)
        assertTrue(lyrics.isNotEmpty())
    }

    @Test
    fun `verbatim lrc keeps word timestamps`() {
        val out = sampleLyrics().toFormat(LyricsFormat.VERBATIMLRC)
        assertTrue(out.contains("[ti:测试歌曲]"))
        assertTrue(out.contains("[ar:测试歌手]"))
        assertTrue(out.contains("[00:01.00]"))
        assertTrue(out.contains("[00:02.00]"))
        // 逐字模式下每个字前都插时间戳，"你好" 不会连续出现
        assertTrue(out.contains("你"))
        assertTrue(out.contains("好"))
        assertTrue(out.contains("[00:04.00]世界"))
    }

    @Test
    fun `line by line lrc drops word timestamps`() {
        val out = sampleLyrics().toFormat(LyricsFormat.LINEBYLINELRC)
        assertTrue(out.contains("[00:01.00]你好"))
        assertTrue(out.contains("[00:04.00]世界"))
    }

    @Test
    fun `enhanced lrc wraps words in angle brackets`() {
        val out = sampleLyrics().toFormat(LyricsFormat.ENHANCEDLRC)
        assertTrue(out.contains("<00:01.00>你"))
        assertTrue(out.contains("<00:02.00>好"))
    }

    @Test
    fun `srt output has index and arrow timing`() {
        val out = sampleLyrics().toFormat(LyricsFormat.SRT)
        assertTrue(out.contains("1"))
        assertTrue(out.contains("00:00:01,000 --> 00:00:03,000"))
        assertTrue(out.contains("00:00:04,000 --> 00:00:06,000"))
        assertTrue(out.contains("你好"))
    }

    @Test
    fun `ass output has script info and dialogue lines`() {
        val out = sampleLyrics().toFormat(LyricsFormat.ASS)
        assertTrue(out.contains("[Script Info]"))
        assertTrue(out.contains("[V4+ Styles]"))
        assertTrue(out.contains("[Events]"))
        assertTrue(out.contains("Dialogue: 0,0:00:01.00,0:00:03.00,Default"))
    }

    @Test
    fun `empty lyrics is reported as empty`() {
        val empty = Lyrics(
            info = LyricInfo(source = Source.QM, songinfo = SongInfo(source = Source.QM))
        )
        assertTrue(empty.isEmpty())
    }
}
