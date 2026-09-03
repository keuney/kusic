package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-093 인수 조건: controller가 실제 player를 움직이고, 탐색 뒤에도 진행이 계속 맞물린다.
 * 표시 위치 규칙 자체는 PendingSeekTest가 일반 단위 검사로 다룬다.
 */
@HiltAndroidTest
class SeekTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun seekingMovesThePlayerBothWaysAndProgressKeepsUp(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync { connection.play() }
            withTimeout(15_000) { connection.playback.first { it.isPlaying && it.positionMs > 500 } }
            val duration = connection.playback.value.durationMs
            assertTrue("내장 음원 길이를 얻지 못함: $duration", duration in 119_000..121_000)

            // 앞으로 탐색: controller 호출이 실제 위치를 옮긴다.
            instrumentation.runOnMainSync { connection.seekTo(60_000) }
            withTimeout(5_000) { connection.playback.first { it.positionMs in 59_500..61_500 } }

            // 탐색 뒤에도 진행이 이어진다.
            val afterForward = connection.playback.value.positionMs
            delay(1_500)
            val advanced = connection.playback.value
            assertTrue(
                "탐색 뒤 진행이 멈춤: $afterForward → ${advanced.positionMs}",
                advanced.positionMs > afterForward + 800,
            )
            assertTrue("탐색 뒤 재생 상태가 아님", advanced.isPlaying)

            // 뒤로 탐색도 같아야 한다.
            instrumentation.runOnMainSync { connection.seekTo(10_000) }
            withTimeout(5_000) { connection.playback.first { it.positionMs in 9_500..11_500 } }
            val afterBackward = connection.playback.value.positionMs
            delay(1_500)
            assertTrue(
                "뒤로 탐색 뒤 진행이 멈춤: $afterBackward → ${connection.playback.value.positionMs}",
                connection.playback.value.positionMs > afterBackward + 800,
            )

            // 음수는 처음으로, 길이를 넘는 값은 끝으로 좁힌다.
            instrumentation.runOnMainSync { connection.pause() }
            withTimeout(5_000) { connection.playback.first { !it.isPlaying } }
            instrumentation.runOnMainSync { connection.seekTo(-5_000) }
            withTimeout(5_000) { connection.playback.first { it.positionMs < 1_000 } }
            assertEquals(0L, connection.playback.value.positionMs.coerceAtMost(999) / 1_000)

            instrumentation.runOnMainSync { connection.seekTo(duration + 30_000) }
            withTimeout(5_000) { connection.playback.first { it.positionMs > duration - 1_500 } }
            assertTrue(
                "길이를 넘겨 탐색했는데 위치가 길이를 넘음: ${connection.playback.value.positionMs} / $duration",
                connection.playback.value.positionMs <= duration,
            )
        } finally {
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
    }
}
