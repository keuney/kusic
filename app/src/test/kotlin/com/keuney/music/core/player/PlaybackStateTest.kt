package com.keuney.music.core.player

import androidx.media3.common.C
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
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
}
