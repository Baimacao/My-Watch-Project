package com.example.lddc.common.models

import com.example.lddc.common.models.info.Artist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistTest {

    @Test
    fun `splits multiple separators`() {
        val artist = Artist.fromString("A/B&C,D、E")
        assertEquals(listOf("A", "B", "C", "D", "E"), artist.toList())
    }

    @Test
    fun `removes duplicates and blanks`() {
        val artist = Artist.fromString("A/A// B ")
        assertEquals(listOf("A", "B"), artist.toList())
    }

    @Test
    fun `blank string yields empty artist`() {
        assertTrue(Artist.fromString(null).isEmpty())
        assertTrue(Artist.fromString("   ").isEmpty())
    }

    @Test
    fun `toString joins with slash`() {
        assertEquals("A/B", Artist.fromString("A/B").toString())
        assertEquals("A-B", Artist.fromString("A/B").toString("-"))
    }
}
