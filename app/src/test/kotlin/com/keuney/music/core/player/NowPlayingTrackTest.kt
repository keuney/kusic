package com.keuney.music.core.player

import com.keuney.music.core.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** KM-112: 즐겨찾기에 넣을 수 있도록 세션이 준 것만으로 곡을 만든다. */
class NowPlayingTrackTest {
    @Test
    fun theSessionMetadataBecomesATrack() {
        val track = NowPlaying("t1", "제목", "아티스트", "https://example.invalid/a.jpg")
            .toTrack(durationMs = 222_000)

        assertEquals("t1", track.id)
        assertEquals("제목", track.title)
        assertEquals("아티스트", track.artist)
        assertEquals("https://example.invalid/a.jpg", track.artworkUrl)
        assertEquals(222_000L, track.durationMs)
        assertEquals(SourceType.Remote, track.source)
    }

    @Test
    fun anUnknownDurationIsLeftOut() {
        // 0을 넣으면 화면이 "0:00"을 사실처럼 보여준다.
        assertNull(NowPlaying("t1", "제목", "아티스트").toTrack(durationMs = 0).durationMs)
        assertNull(NowPlaying("t1", "제목", "아티스트").toTrack(durationMs = -1).durationMs)
    }

    @Test
    fun aMissingArtworkStaysMissing() {
        assertNull(NowPlaying("t1", "제목", "아티스트").toTrack(durationMs = 1_000).artworkUrl)
    }
}
