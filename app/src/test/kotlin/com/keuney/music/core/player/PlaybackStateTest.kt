package com.keuney.music.core.player

import androidx.media3.common.C
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun focusSuppressionIsPausedEvenWhenPlaybackIsRequested() {
        val suppressed = mapPlaybackState(Player.STATE_READY, false, true, 500, 1000, false)
        assertEquals(PlaybackPhase.Paused, suppressed.phase)
        assertTrue(suppressed.playWhenReady)
        val resumed = mapPlaybackState(Player.STATE_READY, true, true, 500, 1000, false)
        assertEquals(PlaybackPhase.Playing, resumed.phase)
    }

    @Test
    fun mapsReadinessPlaybackAndErrorToUserStates() {
        val cases = listOf(
            Triple(Player.STATE_IDLE, false, PlaybackPhase.Idle),
            Triple(Player.STATE_BUFFERING, false, PlaybackPhase.Buffering),
            Triple(Player.STATE_READY, true, PlaybackPhase.Playing),
            Triple(Player.STATE_READY, false, PlaybackPhase.Paused),
            Triple(Player.STATE_ENDED, false, PlaybackPhase.Ended),
        )
        cases.forEach { (state, playing, expected) ->
            assertEquals(expected, mapPlaybackState(state, playing, playing, 100, 1000, false).phase)
        }
        assertEquals(
            PlaybackPhase.Unavailable,
            mapPlaybackState(Player.STATE_IDLE, false, false, 0, 0, true).phase,
        )
    }

    @Test
    fun normalizesUnknownDurationAndOutOfRangePosition() {
        assertEquals(0L, mapPlaybackState(Player.STATE_IDLE, false, false, 500, C.TIME_UNSET, false).durationMs)
        assertEquals(0L, mapPlaybackState(Player.STATE_IDLE, false, false, 500, C.TIME_UNSET, false).positionMs)
        assertEquals(0L, mapPlaybackState(Player.STATE_READY, false, false, -20, 1000, false).positionMs)
        assertEquals(1000L, mapPlaybackState(Player.STATE_READY, true, true, 2000, 1000, false).positionMs)
    }

    @Test
    fun playingAndBufferingFollowThePhase() {
        val playing = mapPlaybackState(Player.STATE_READY, true, true, 100, 1000, false)
        assertTrue(playing.isPlaying)
        assertFalse(playing.isBuffering)

        val buffering = mapPlaybackState(Player.STATE_BUFFERING, false, true, 100, 1000, false)
        assertTrue(buffering.isBuffering)
        assertFalse(buffering.isPlaying)

        val paused = mapPlaybackState(Player.STATE_READY, false, false, 100, 1000, false)
        assertFalse(paused.isPlaying)
        assertFalse(paused.isBuffering)
    }

    @Test
    fun mapsEveryRepeatModeAndUnknownValuesFallBackToOff() {
        assertEquals(RepeatMode.Off, repeatModeOf(Player.REPEAT_MODE_OFF))
        assertEquals(RepeatMode.One, repeatModeOf(Player.REPEAT_MODE_ONE))
        assertEquals(RepeatMode.All, repeatModeOf(Player.REPEAT_MODE_ALL))
        // 알 수 없는 상수가 오면 반복을 켠 것으로 보지 않는다.
        assertEquals(RepeatMode.Off, repeatModeOf(99))
    }

    @Test
    fun shuffleIsCarriedThrough() {
        assertTrue(
            mapPlaybackState(Player.STATE_READY, true, true, 0, 1000, false, shuffleEnabled = true)
                .shuffleEnabled,
        )
        assertFalse(
            mapPlaybackState(Player.STATE_READY, true, true, 0, 1000, false, shuffleEnabled = false)
                .shuffleEnabled,
        )
    }

    @Test
    fun theDefaultStateHasNoTrackNoRepeatAndNoShuffle() {
        val state = PlaybackState()
        assertNull(state.nowPlaying)
        assertEquals(RepeatMode.Off, state.repeatMode)
        assertFalse(state.shuffleEnabled)
    }

    @Test
    fun theCurrentQueueItemBecomesTheCurrentTrack() {
        assertEquals(
            NowPlaying("track-1", "제목", "아티스트"),
            nowPlayingOf("track-1", "제목", "아티스트"),
        )
    }

    @Test
    fun anEmptyQueueHasNoCurrentTrack() {
        assertNull("ID가 없으면 현재 곡이 없다", nowPlayingOf(null, "제목", "아티스트"))
        // Media3의 기본 mediaId는 빈 문자열이라 곡이 없는 것과 구분되지 않는다.
        assertNull(nowPlayingOf("", "제목", "아티스트"))
        assertNull(nowPlayingOf("   ", "제목", "아티스트"))
    }

    @Test
    fun missingTitleOrArtistBecomesEmptyRatherThanNull() {
        assertEquals(NowPlaying("track-1", "", ""), nowPlayingOf("track-1", null, null))
        assertEquals(NowPlaying("track-1", "제목", ""), nowPlayingOf("track-1", "  제목  ", "  "))
    }

    @Test
    fun theCurrentTrackReachesTheState() {
        val state = mapPlaybackState(
            Player.STATE_READY,
            true,
            true,
            100,
            1000,
            false,
            nowPlaying = NowPlaying("track-1", "제목", "아티스트"),
            repeatMode = Player.REPEAT_MODE_ALL,
            shuffleEnabled = true,
        )
        assertEquals(NowPlaying("track-1", "제목", "아티스트"), state.nowPlaying)
        assertEquals(RepeatMode.All, state.repeatMode)
        assertTrue(state.shuffleEnabled)
    }

    private fun repeatModeOf(repeatMode: Int) =
        mapPlaybackState(Player.STATE_READY, true, true, 0, 1000, false, repeatMode = repeatMode)
            .repeatMode
}
