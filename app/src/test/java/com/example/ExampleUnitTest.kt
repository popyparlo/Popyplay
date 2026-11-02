package com.example

import com.example.data.model.LyricsParser
import com.example.data.model.PlaylistType
import com.example.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testDurationFormatting() {
    val track = Track(
      id = 1L,
      title = "Test Song",
      artist = "Test Artist",
      album = "Test Album",
      durationMs = 190_000L,
      contentUri = "content://media/test"
    )
    assertEquals("03:10", track.durationFormatted)
  }

  @Test
  fun testLyricsParsing() {
    val sampleLrc = """
      [00:00.00] Already under my skin
      [00:04.50] And I know exactly how this ends
    """.trimIndent()

    val parsed = LyricsParser.parseLrc(sampleLrc)
    assertEquals(2, parsed.size)
    assertEquals(0L, parsed[0].timeMs)
    assertEquals("Already under my skin", parsed[0].text)
    assertEquals(4500L, parsed[1].timeMs)
    assertEquals("And I know exactly how this ends", parsed[1].text)
  }

  @Test
  fun testPlaylistTypes() {
    assertTrue(PlaylistType.values().contains(PlaylistType.SINGLE_FILE))
  }
}
