package com.keuney.music.ui.components

import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSubtitleTest {
    @Test
    fun artistAndKnownDurationAreJoined() {
        assertEquals("1theK (원더케이) · 3:42", subtitleOf(artist = "1theK (원더케이)", durationMs = 222_000))
    }

    @Test
    fun unknownDurationLeavesArtistAlone() {
        assertEquals("1theK (원더케이)", subtitleOf(artist = "1theK (원더케이)", durationMs = null))
    }

    @Test
    fun missingArtistLeavesDurationAlone() {
        assertEquals("3:42", subtitleOf(artist = "", durationMs = 222_000))
    }

    @Test
    fun blankArtistIsTreatedAsMissing() {
        assertEquals("3:42", subtitleOf(artist = "   ", durationMs = 222_000))
    }

    @Test
    fun nothingKnownGivesEmptyLine() {
        assertEquals("", subtitleOf(artist = "", durationMs = null))
    }

    @Test
    fun durationOverAnHourKeepsCountingMinutes() {
        assertEquals("딩고 뮤직 · 18:24", subtitleOf(artist = "딩고 뮤직", durationMs = 1_104_000))
    }

    private fun subtitleOf(artist: String, durationMs: Long?) = trackSubtitle(
        Track(
            id = "id",
            title = "제목",
            artist = artist,
            artworkUrl = null,
            durationMs = durationMs,
            source = SourceType.Remote,
        ),
    )
}
