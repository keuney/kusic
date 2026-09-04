package com.keuney.music.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KM-138 인수 조건의 규칙. 무엇을 넘기고 무엇을 넘기지 않는지만 다룬다. 실제로 넘기는 일은
 * MusicService가 하고 실기기 계측이 확인한다.
 */
class UnplayableSkipTest {
    @Test
    fun anUnplayableTrackIsSkippedWhenSomethingFollowsIt() {
        assertTrue(skip().shouldSkip(PlaybackFailure.Source, playWhenReady = true, hasNext = true, queueSize = 3))
    }

    @Test
    fun theLastTrackIsNotSkipped() {
        // 넘어갈 곳이 없다. 그때는 문구와 다시 시도가 남는다.
        assertFalse(skip().shouldSkip(PlaybackFailure.Source, playWhenReady = true, hasNext = false, queueSize = 3))
    }

    @Test
    fun networkFailuresWaitInsteadOfSkipping() {
        // 연결이 돌아오면 그 곡을 이어 들어야 한다(ADR-063). 넘기면 듣던 곡을 잃는다.
        assertFalse(skip().shouldSkip(PlaybackFailure.Network, playWhenReady = true, hasNext = true, queueSize = 3))
    }

    @Test
    fun aBlockedTrackIsNotSkipped() {
        // WiFi 전용 재생이 막은 것이다. 곡의 문제가 아니므로 넘겨도 같은 일이 반복된다.
        assertFalse(skip().shouldSkip(PlaybackFailure.Blocked, playWhenReady = true, hasNext = true, queueSize = 3))
    }

    @Test
    fun pausedPlaybackIsNotAdvanced() {
        // 멈춰 둔 재생을 넘겨 가며 이어 트는 것은 참견이다.
        assertFalse(skip().shouldSkip(PlaybackFailure.Source, playWhenReady = false, hasNext = true, queueSize = 3))
    }

    @Test
    fun noFailureIsNoSkip() {
        assertFalse(skip().shouldSkip(null, playWhenReady = true, hasNext = true, queueSize = 3))
    }

    @Test
    fun aQueueWhereEverythingFailsIsWalkedOnceAndThenLeftAlone() {
        // 전체 반복이면 마지막 곡 뒤에 처음으로 돌아오므로 "다음 곡이 있다"가 늘 참이다.
        // 그때 끝없이 도는 것을 막는 것이 횟수 제한의 목적이다.
        val skip = skip()
        repeat(3) {
            assertTrue("$it 번째 넘김", skip.shouldSkip(PlaybackFailure.Source, true, hasNext = true, queueSize = 3))
        }
        assertFalse("대기열을 한 번 훑었으면 멈춘다", skip.shouldSkip(PlaybackFailure.Source, true, true, 3))
    }

    @Test
    fun playingAgainGivesBackTheAllowance() {
        val skip = skip()
        assertTrue(skip.shouldSkip(PlaybackFailure.Source, true, hasNext = true, queueSize = 1))
        assertFalse("한 곡짜리 대기열은 한 번이면 다 훑었다", skip.shouldSkip(PlaybackFailure.Source, true, true, 1))

        // 한 번 재생됐으면 그다음 실패는 다시 제 몫의 넘김을 갖는다.
        skip.onPlayed()
        assertTrue(skip.shouldSkip(PlaybackFailure.Source, true, hasNext = true, queueSize = 1))
    }

    private fun skip() = UnplayableSkip()
}
