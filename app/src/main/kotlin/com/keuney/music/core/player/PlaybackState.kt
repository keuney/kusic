package com.keuney.music.core.player

import androidx.media3.common.Player

internal enum class PlaybackPhase { Idle, Buffering, Playing, Paused, Ended, Unavailable }

internal data class PlaybackState(
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val playWhenReady: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

internal fun mapPlaybackState(
    playerState: Int,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasError: Boolean,
): PlaybackState {
    val phase = when {
        hasError -> PlaybackPhase.Unavailable
        playerState == Player.STATE_BUFFERING -> PlaybackPhase.Buffering
        playerState == Player.STATE_ENDED -> PlaybackPhase.Ended
        isPlaying -> PlaybackPhase.Playing
        playerState == Player.STATE_READY -> PlaybackPhase.Paused
        else -> PlaybackPhase.Idle
    }
    val duration = durationMs.coerceAtLeast(0)
    return PlaybackState(
        phase = phase,
        playWhenReady = playWhenReady,
        positionMs = if (duration > 0) positionMs.coerceIn(0, duration) else 0,
        durationMs = duration,
    )
}
