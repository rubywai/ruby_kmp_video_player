package io.github.waiphyoaung.rubykmpplayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class RubyPlayerModelTest {
    @Test
    fun playbackConfigDefaultsAndValidatesOptions() {
        val config = RubyPlayerConfig(volume = 0.5f, playbackSpeed = 1.5f)

        assertEquals(0.5f, config.volume)
        assertEquals(1.5f, config.playbackSpeed)
    }

    @Test
    fun playbackConfigRejectsInvalidOptions() {
        assertFailsWith<IllegalArgumentException> { RubyPlayerConfig(volume = -0.1f) }
        assertFailsWith<IllegalArgumentException> { RubyPlayerConfig(volume = 1.1f) }
        assertFailsWith<IllegalArgumentException> { RubyPlayerConfig(playbackSpeed = 0f) }
        assertFailsWith<IllegalArgumentException> { RubyPlayerConfig(startPositionMs = -1L) }
    }

    @Test
    fun configDefaultsAreConservative() {
        val config = RubyPlayerConfig()

        assertFalse(config.autoPlay)
        assertFalse(config.looping)
        assertEquals(0L, config.startPositionMs)
    }

    @Test
    fun sourceDefaultsToAutoContentTypeAndNoHeaders() {
        val source = RubyVideoSource(url = "https://example.com/video.mp4")

        assertEquals(RubyVideoContentType.Auto, source.contentType)
        assertEquals(emptyMap(), source.headers)
    }
}
