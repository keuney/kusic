package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-090: 실제 MediaController가 내놓는 값이 화면 상태로 옮겨지는지 확인한다.
 * 매핑 규칙 자체는 PlaybackStateTest가 일반 단위 검사로 다룬다.
 */
@HiltAndroidTest
class PlaybackStateAdapterTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun theSessionStateBecomesTheScreenState(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            assertNull("연결 전에는 현재 곡이 없다", connection.playback.value.nowPlaying)

            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }

            // 서비스 기본 대기열은 내장 테스트 음원이다.
            val loaded = withTimeout(10_000) { connection.playback.first { it.nowPlaying != null } }
            val nowPlaying = requireNotNull(loaded.nowPlaying)
            assertEquals(MusicService.TEST_TONE_MEDIA_ID, nowPlaying.mediaId)
            assertTrue("제목이 비어 있음", nowPlaying.title.isNotBlank())
            assertTrue("아티스트가 비어 있음", nowPlaying.artist.isNotBlank())

            // 반복과 셔플은 세션의 실제 값이며 기본은 꺼짐이다. 켜는 조작은 KM-095·096 범위다.
            assertEquals(RepeatMode.Off, loaded.repeatMode)
            assertFalse(loaded.shuffleEnabled)

            instrumentation.runOnMainSync { connection.play() }
            val playing = withTimeout(15_000) {
                connection.playback.first { it.isPlaying && it.positionMs > 500 }
            }
            assertFalse("재생 중에는 준비 중이 아니다", playing.isBuffering)
            assertTrue("길이가 오지 않았다", playing.durationMs in 119_000..121_000)
            assertEquals(MusicService.TEST_TONE_MEDIA_ID, playing.nowPlaying?.mediaId)

            instrumentation.runOnMainSync { connection.pause() }
            val paused = withTimeout(5_000) { connection.playback.first { it.phase == PlaybackPhase.Paused } }
            assertFalse(paused.isPlaying)
            assertTrue("일시정지에도 위치와 현재 곡은 남는다", paused.positionMs > 0)
            assertNotNull(paused.nowPlaying)
        } finally {
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
        // 연결을 끊으면 화면이 곡 정보를 계속 들고 있지 않는다.
        assertNull(connection.playback.value.nowPlaying)
    }
}
