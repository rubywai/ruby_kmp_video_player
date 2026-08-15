package io.github.waiphyoaung.rubykmpplayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RubyPlayerModelTest {
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
