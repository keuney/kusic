package com.keuney.music.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KM-132 인수 조건 중 "합리적인 회복"의 규칙이다. 무엇을 언제 이어 붙일지만 다룬다. 실제로
 * 이어 붙이는 일은 MusicService가 하고 실기기에서 확인한다.
 */
class NetworkRecoveryTest {
    @Test
    fun playbackCutByTheNetworkResumesWhenItComesBack() {
        val recovery = NetworkRecovery()
        recovery.onError(PlaybackFailure.Network, playWhenReady = true)
        assertTrue(recovery.onNetworkAvailable())
    }

    @Test
    fun playbackStoppedByTheUserIsNotResumed() {
        // 멈춰 둔 재생을 연결이 돌아왔다고 다시 트는 것은 회복이 아니라 참견이다.
        val recovery = NetworkRecovery()
        recovery.onError(PlaybackFailure.Network, playWhenReady = false)
        assertFalse(recovery.onNetworkAvailable())
    }

    @Test
    fun aTrackThatCannotBePlayedIsNotRetried() {
        // 곡 자체를 가져올 수 없으면 연결이 돌아와도 결과가 같다.
        val recovery = NetworkRecovery()
        recovery.onError(PlaybackFailure.Source, playWhenReady = true)
        assertFalse(recovery.onNetworkAvailable())
    }

    @Test
    fun nothingIsResumedWithoutAFailure() {
        assertFalse(NetworkRecovery().onNetworkAvailable())
    }

    @Test
    fun aFlappingConnectionStopsBeingRetried() {
        // 붙었다 끊겼다 하는 곳에서 같은 실패를 끝없이 되풀이하지 않는다.
        val recovery = NetworkRecovery(maxAttempts = 2)
        recovery.onError(PlaybackFailure.Network, playWhenReady = true)
        assertTrue(recovery.onNetworkAvailable())
        assertTrue(recovery.onNetworkAvailable())
        assertFalse("정해진 횟수를 넘겨 다시 시도했다", recovery.onNetworkAvailable())
    }

    @Test
    fun playingAgainGivesBackTheAttempts() {
        val recovery = NetworkRecovery(maxAttempts = 1)
        recovery.onError(PlaybackFailure.Network, playWhenReady = true)
        assertTrue(recovery.onNetworkAvailable())

        // 한 번 회복하면 처음 상태로 돌아간다. 다음 끊김은 다시 제 몫의 시도를 갖는다.
        recovery.onReady()
        assertFalse("회복한 뒤에는 기다리는 재생이 없다", recovery.onNetworkAvailable())
        recovery.onError(PlaybackFailure.Network, playWhenReady = true)
        assertTrue(recovery.onNetworkAvailable())
    }
}
