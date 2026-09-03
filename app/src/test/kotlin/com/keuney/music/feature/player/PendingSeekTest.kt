package com.keuney.music.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** KM-093: 손을 뗀 뒤에도 표시가 되돌아가지 않고 실제 위치와 다시 맞물리는지 확인한다. */
class PendingSeekTest {
    @Test
    fun theFingerWinsWhileDragging() {
        assertEquals(
            42_000L,
            seekDisplayPositionMs(reportedMs = 5_000, draggingMs = 42_000, pending = null),
        )
        // 끌고 있는 중이면 이전 탐색 목표보다도 손가락이 먼저다.
        assertEquals(
            42_000L,
            seekDisplayPositionMs(
                reportedMs = 5_000,
                draggingMs = 42_000,
                pending = PendingSeek(fromMs = 5_000, toMs = 30_000),
            ),
        )
    }

    @Test
    fun withoutASeekTheRealPositionIsShown() {
        assertEquals(5_000L, seekDisplayPositionMs(reportedMs = 5_000, draggingMs = null, pending = null))
    }

    @Test
    fun theTargetIsShownUntilThePlayerGetsThere() {
        val pending = PendingSeek(fromMs = 5_000, toMs = 60_000)

        // 손을 뗀 직후 위치 보고는 아직 탐색 이전 자리다. 그대로 쓰면 슬라이더가 되돌아간다.
        assertEquals(60_000L, seekDisplayPositionMs(reportedMs = 5_000, draggingMs = null, pending = pending))
        assertFalse(pending.isSettled(5_000))

        // 도달하면 표시를 실제 위치에 넘긴다.
        assertTrue(pending.isSettled(60_100))
        assertEquals(60_100L, seekDisplayPositionMs(reportedMs = 60_100, draggingMs = null, pending = pending))
    }

    @Test
    fun aBackwardSeekBehavesTheSameWay() {
        val pending = PendingSeek(fromMs = 100_000, toMs = 30_000)

        assertEquals(30_000L, seekDisplayPositionMs(reportedMs = 100_000, draggingMs = null, pending = pending))
        assertFalse(pending.isSettled(100_000))
        assertTrue(pending.isSettled(30_200))
    }

    @Test
    fun playbackPassingTheTargetCountsAsSettled() {
        val pending = PendingSeek(fromMs = 5_000, toMs = 60_000)

        // 느린 보고 사이에 목표를 지나쳐 버렸어도 표시를 붙잡고 있지 않는다.
        assertTrue(pending.isSettled(63_000))
    }

    @Test
    fun aSeekThatNeverLandsDoesNotFreezeTheDisplay() {
        val pending = PendingSeek(fromMs = 100_000, toMs = 30_000)

        // 탐색이 받아들여지지 않고 재생이 그대로 나아가면 목표에서 멀어진다. 그때 놓아준다.
        assertTrue(pending.isSettled(101_000))
        assertEquals(101_000L, seekDisplayPositionMs(reportedMs = 101_000, draggingMs = null, pending = pending))
    }

    @Test
    fun aReportWithinToleranceIsAlreadySettled() {
        val pending = PendingSeek(fromMs = 0, toMs = 30_000)

        assertTrue(pending.isSettled(29_100))
        assertTrue(pending.isSettled(30_900))
        assertFalse(pending.isSettled(28_000))
    }
}
