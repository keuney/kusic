package com.keuney.music.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayableStreamTest {
    @Test
    fun diagnosticRepresentationDoesNotExposeTheStreamOrItsQuery() {
        val url = "https://example.invalid/audio?token=sensitive-test-value"
        val stream = PlayableStream(url, "audio/webm", 128_000)

        assertEquals(url, stream.url)
        assertEquals("PlayableStream(url=<redacted>)", stream.toString())
        assertFalse(stream.toString().contains("sensitive-test-value"))
    }
}
