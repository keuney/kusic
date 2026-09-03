package com.keuney.music.core.player

/**
 * 재생 기록을 남길 시점을 정하는 규칙.
 *
 * 재생을 시작한 순간이 아니라 얼마 이상 들었을 때 남긴다. 훑어보며 넘긴 곡까지 남으면 "최근
 * 재생"이 방금 스쳐 간 목록이 되어 쓸모가 없다.
 */
internal object PlaybackHistory {
    /** 이 정도 들으면 남긴다. */
    private const val LISTENED_MS = 10_000L

    /**
     * 남기기까지 기다릴 시간.
     *
     * 곡이 기준보다 짧으면 절반만 들어도 남긴다. 그렇게 하지 않으면 짧은 곡은 끝까지 들어도
     * 기록되지 않는다. 길이를 아직 모르면 기준을 그대로 쓴다.
     */
    fun listenedThresholdMs(durationMs: Long): Long = when {
        durationMs <= 0 -> LISTENED_MS
        durationMs < LISTENED_MS * 2 -> durationMs / 2
        else -> LISTENED_MS
    }
}
