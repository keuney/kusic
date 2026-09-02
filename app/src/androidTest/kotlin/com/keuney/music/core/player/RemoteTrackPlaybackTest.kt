package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-057 인수 조건: Track ID → resolveStream → ExoPlayer → playback.
 * 기기의 실제 네트워크가 필요하며 공급자 응답에 의존한다.
 */
@HiltAndroidTest
class RemoteTrackPlaybackTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun playsARemoteTrackFromItsIdWithoutTheCallerKnowingTheStreamUrl(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync {
                connection.playTrack(TRACK_ID, "원격 스트림 확인용 트랙", "Keuney Music")
            }
            withTimeout(40_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 1_000 }
            }
            val started = connection.playback.value
            assertTrue("원격 트랙 길이를 얻지 못함", started.durationMs > 0)
            delay(3_000)
            val advanced = connection.playback.value
            assertTrue(
                "재생 위치가 진행하지 않음: ${started.positionMs} → ${advanced.positionMs}",
                advanced.positionMs > started.positionMs + 2_000,
            )
            assertTrue("재생 중 오류 상태", advanced.phase == PlaybackPhase.Playing)
        } finally {
            // 서비스와 대기열은 다른 계측 테스트와 공유되므로 내장 테스트 음원으로 되돌린다.
            instrumentation.runOnMainSync {
                connection.pause()
                connection.playTrack(MusicService.TEST_TONE_MEDIA_ID, "테스트 오디오", "Keuney Music")
            }
            runCatching {
                withTimeout(10_000) { connection.playback.first { it.durationMs > 100_000 } }
            }
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val TRACK_ID = "gdZLi9oWNZg"
    }
}
