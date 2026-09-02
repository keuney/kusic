package com.keuney.music.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackUriTest {
    @Test
    fun buildsAPlaceholderThatCarriesOnlyTheTrackId() {
        val uri = TrackUri.of("gdZLi9oWNZg")
        assertTrue(uri.startsWith("${TrackUri.SCHEME}://"))
        assertTrue(uri.endsWith("gdZLi9oWNZg"))
        assertEquals("gdZLi9oWNZg", TrackUri.trackIdOrNull(uri))
    }

    @Test
    fun readsBackOnlyItsOwnScheme() {
        assertNull(TrackUri.trackIdOrNull("https://example.invalid/audio"))
        assertNull(TrackUri.trackIdOrNull("android.resource://com.keuney.music/1"))
        assertNull(TrackUri.trackIdOrNull("${TrackUri.SCHEME}://track/"))
        assertNull(TrackUri.trackIdOrNull("${TrackUri.SCHEME}://track/bad id"))
        assertNull(TrackUri.trackIdOrNull("${TrackUri.SCHEME}://track/a/b"))
    }

    @Test
    fun rejectsIdsThatWouldChangeTheRequestTarget() {
        for (bad in listOf("", " ", "../other", "a?b", "a#b", "a/b", "a".repeat(65))) {
            val failed = runCatching { TrackUri.of(bad) }.isFailure
            assertTrue("허용하면 안 되는 Track ID: '$bad'", failed)
        }
    }
}
