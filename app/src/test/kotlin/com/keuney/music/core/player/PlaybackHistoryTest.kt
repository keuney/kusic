package com.keuney.music.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** KM-115: 얼마나 들었을 때 기록으로 남길지 정하는 규칙. */
class PlaybackHistoryTest {
    @Test
    fun aNormalTrackIsRecordedAfterTenSeconds() {
        assertEquals(10_000L, PlaybackHistory.listenedThresholdMs(180_000))
        assertEquals(10_000L, PlaybackHistory.listenedThresholdMs(20_000))
    }

    @Test
    fun aShortTrackIsRecordedAtItsHalfway() {
        // 20초보다 짧은 곡은 끝까지 들어도 10초를 넘지 않을 수 있다.
        assertEquals(6_000L, PlaybackHistory.listenedThresholdMs(12_000))
        assertEquals(1_500L, PlaybackHistory.listenedThresholdMs(3_000))
    }

    @Test
    fun anUnknownLengthUsesTheNormalRule() {
        // 준비되기 전에는 길이를 알 수 없다. 그때는 기준을 그대로 쓴다.
        assertEquals(10_000L, PlaybackHistory.listenedThresholdMs(0))
        assertEquals(10_000L, PlaybackHistory.listenedThresholdMs(-1))
    }
}
