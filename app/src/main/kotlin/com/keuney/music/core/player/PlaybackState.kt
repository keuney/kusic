package com.keuney.music.core.player

import androidx.media3.common.Player

internal enum class PlaybackPhase { Idle, Buffering, Playing, Paused, Ended, Unavailable }

/**
 * ARCHITECTURE 18의 정책대로 반복은 세 가지뿐이다.
 *
 * 설정 저장소 계약에 들어가므로 공개 타입이다. 저장된 값은 이름으로 남으니 상수 이름을 바꾸면
 * 이전에 저장된 설정을 읽지 못한다.
 */
enum class RepeatMode { Off, One, All }

/**
 * 지금 재생 중인 대기열 항목. 세션이 실제로 들고 있는 것만 담는다.
 *
 * 대기열에는 Track ID와 metadata만 넣으므로(AGENTS.md 8) 여기에도 그만큼만 온다. 재생 주소는
 * 여기 오지 않는다.
 */
internal data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: String? = null,
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
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    /**
     * 대기열에 넣은 순서다. 셔플이 켜졌을 때의 실제 재생 순서는 여기 담을 수 없다(ADR-053).
     * 세션이 컨트롤러에 보내는 Timeline에 셔플 순서가 실려 오지 않는다.
     */
    val queue: List<NowPlaying> = emptyList(),
    /** [queue]에서 현재 곡의 자리. 대기열이 비어 있으면 -1이다. */
    val queueIndex: Int = -1,
    /** 재생이 멈춘 이유. 멈추지 않았으면 null이다. 문구와 회복 판단이 이 값 하나를 본다. */
    val failure: PlaybackFailure? = null,
    /**
     * 재생할 수 없어 방금 지나간 곡의 제목(KM-138). 지나간 것이 없으면 null이다.
     *
     * 음악이 저절로 이어지는 것은 좋지만, 무엇이 빠졌는지 말해 주지 않으면 사용자는 대기열이
     * 마음대로 움직였다고 느낀다. 사용자가 다음 조작을 하면 지운다.
     */
    val skippedTitle: String? = null,
) {
    // 재생과 준비는 phase 하나로 정하고 여기서는 이름만 붙인다. 같은 사실을 두 곳에 두지 않는다.
    val isPlaying: Boolean get() = phase == PlaybackPhase.Playing
    val isBuffering: Boolean get() = phase == PlaybackPhase.Buffering

    /**
     * 지금 버튼이 일시정지로 보여야 하는지. 재생을 요청한 상태라도 곡이 끝났거나 재생할 수 없으면
     * 다시 재생을 눌러야 한다. 이 계산을 화면마다 되풀이하지 않는다.
     */
    val canPause: Boolean
        get() = playWhenReady &&
            phase != PlaybackPhase.Ended &&
            phase != PlaybackPhase.Unavailable
}

internal fun mapPlaybackState(
    playerState: Int,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    positionMs: Long,
    durationMs: Long,
    failure: PlaybackFailure?,
    nowPlaying: NowPlaying? = null,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    shuffleEnabled: Boolean = false,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false,
    queue: List<NowPlaying> = emptyList(),
    queueIndex: Int = -1,
    skippedTitle: String? = null,
): PlaybackState {
    val phase = when {
        failure != null -> PlaybackPhase.Unavailable
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
        hasPrevious = hasPrevious,
        hasNext = hasNext,
        queue = queue,
        // 대기열 밖을 가리키는 자리는 없는 것으로 본다.
        queueIndex = queueIndex.takeIf { it in queue.indices } ?: -1,
        failure = failure,
        skippedTitle = skippedTitle?.takeIf(String::isNotBlank),
    )
}

/**
 * 대기열 항목을 화면이 쓸 현재 곡으로 바꾼다. ID가 없으면 대기열이 비어 있는 것으로 본다.
 * 제목이나 아티스트가 없는 항목도 있으므로 없으면 빈 문자열이다.
 */
internal fun nowPlayingOf(
    mediaId: String?,
    title: String?,
    artist: String?,
    artworkUri: String? = null,
): NowPlaying? {
    if (mediaId.isNullOrBlank()) return null
    return NowPlaying(
        mediaId = mediaId,
        title = title.orEmpty().trim(),
        artist = artist.orEmpty().trim(),
        artworkUri = artworkUri?.takeIf(String::isNotBlank),
    )
}

private fun mapRepeatMode(repeatMode: Int): RepeatMode = when (repeatMode) {
    Player.REPEAT_MODE_ONE -> RepeatMode.One
    Player.REPEAT_MODE_ALL -> RepeatMode.All
    else -> RepeatMode.Off
}
