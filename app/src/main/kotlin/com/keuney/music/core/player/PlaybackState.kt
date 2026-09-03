package com.keuney.music.core.player

import androidx.media3.common.Player

internal enum class PlaybackPhase { Idle, Buffering, Playing, Paused, Ended, Unavailable }

/** ARCHITECTURE 18의 정책대로 반복은 세 가지뿐이다. */
internal enum class RepeatMode { Off, One, All }

/**
 * 지금 재생 중인 대기열 항목. 세션이 실제로 들고 있는 것만 담는다.
 *
 * 대기열에는 Track ID와 metadata만 넣으므로(AGENTS.md 8) 여기에도 그만큼만 온다. 앨범 이미지는
 * 아직 대기열 항목에 넣지 않으며, 필요해지는 Now Playing 화면(KM-091·092)에서 함께 다룬다.
 */
internal data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String,
)

/**
 * 화면이 그리는 재생 상태의 전부다. UI는 Media3 상수를 직접 해석하지 않는다(ARCHITECTURE 19).
 */
internal data class PlaybackState(
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val playWhenReady: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val nowPlaying: NowPlaying? = null,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val shuffleEnabled: Boolean = false,
) {
    // 재생과 준비는 phase 하나로 정하고 여기서는 이름만 붙인다. 같은 사실을 두 곳에 두지 않는다.
    val isPlaying: Boolean get() = phase == PlaybackPhase.Playing
    val isBuffering: Boolean get() = phase == PlaybackPhase.Buffering
}

internal fun mapPlaybackState(
    playerState: Int,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    positionMs: Long,
    durationMs: Long,
    hasError: Boolean,
    nowPlaying: NowPlaying? = null,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    shuffleEnabled: Boolean = false,
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
        nowPlaying = nowPlaying,
        repeatMode = mapRepeatMode(repeatMode),
        shuffleEnabled = shuffleEnabled,
    )
}

/**
 * 대기열 항목을 화면이 쓸 현재 곡으로 바꾼다. ID가 없으면 대기열이 비어 있는 것으로 본다.
 * 제목이나 아티스트가 없는 항목도 있으므로 없으면 빈 문자열이다.
 */
internal fun nowPlayingOf(mediaId: String?, title: String?, artist: String?): NowPlaying? {
    if (mediaId.isNullOrBlank()) return null
    return NowPlaying(
        mediaId = mediaId,
        title = title.orEmpty().trim(),
        artist = artist.orEmpty().trim(),
    )
}

private fun mapRepeatMode(repeatMode: Int): RepeatMode = when (repeatMode) {
    Player.REPEAT_MODE_ONE -> RepeatMode.One
    Player.REPEAT_MODE_ALL -> RepeatMode.All
    else -> RepeatMode.Off
}
