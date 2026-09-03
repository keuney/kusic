package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-114 인수 조건: 재생목록이 대기열이 되고, 곡이 끝나면 다음으로 이어지고, 이전·다음이 곡 사이를
 * 움직인다.
 *
 * 곡이 여러 개인 대기열이 필요하다. KM-094는 대기열에 한 곡뿐이어서 곡 사이 이동을 확인할 수
 * 없었고 그 확인이 여기로 넘어왔다.
 *
 * 내장 테스트 음원을 두 번 넣어 쓴다. 실제 소리가 나고 네트워크가 필요하지 않으며 끝까지 기다릴
 * 수 있다. 두 항목의 ID가 같으므로 어디에 있는지는 `queueIndex`로 본다.
 */
@HiltAndroidTest
class PlaylistPlaybackTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun aMultiTrackQueuePlaysThroughAndMovesBothWays(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            assertNotNull(
                "세션에 연결되지 않았다",
                withTimeoutOrNull(15_000) { connection.state.first { it == ConnectionState.Connected } },
            )

            // 재생목록에서 곡을 고른 것과 같은 경로다. 목록 전체가 대기열이 된다.
            instrumentation.runOnMainSync {
                connection.playQueue(listOf(testTone(), testTone()), startIndex = 0)
                connection.pause()
            }
            // 명령 가용성은 대기열·자리와 같은 순간에 오지 않는다. 즉시 읽지 않고 기다린다.
            assertNotNull(
                "대기열 두 곡과 다음 가용성이 상태로 오지 않았다",
                await(connection) {
                    it.queue.size == 2 && it.durationMs > 100_000 && it.queueIndex == 0 && it.hasNext
                },
            )
            val duration = connection.playback.value.durationMs

            // 다음: 곡 사이를 움직인다.
            instrumentation.runOnMainSync { connection.seekToNext() }
            assertNotNull(
                "다음으로 넘어가지 않았거나 마지막 곡인데 다음을 쓸 수 있다",
                await(connection) { it.queueIndex == 1 && !it.hasNext },
            )

            // 이전: 곡을 막 시작한 자리에서는 앞 곡으로 간다.
            instrumentation.runOnMainSync { connection.seekToPrevious() }
            assertNotNull(
                "이전으로 앞 곡에 가지 않았다",
                await(connection) { it.queueIndex == 0 },
            )

            // 이전: 한참 들은 자리에서는 그 곡의 처음으로 되돌린다(KM-094와 같은 규칙).
            instrumentation.runOnMainSync { connection.seekTo(40_000) }
            assertNotNull("탐색이 반영되지 않았다", await(connection) { it.positionMs > 39_000 })
            instrumentation.runOnMainSync { connection.seekToPrevious() }
            assertNotNull(
                "한참 들은 자리에서 이전이 곡의 처음으로 되돌리지 않았다",
                await(connection) { it.positionMs < 3_000 },
            )
            assertEquals("이전이 대기열 자리를 옮겼다", 0, connection.playback.value.queueIndex)

            // 이어 듣기: 곡이 끝나면 다음 곡이 저절로 시작한다.
            instrumentation.runOnMainSync {
                connection.seekTo(duration - 2_000)
                connection.play()
            }
            assertNotNull(
                "곡이 끝난 뒤 다음 곡으로 이어지지 않았다",
                await(connection, 30_000) { it.queueIndex == 1 && it.isPlaying },
            )
            delay(1_000)
            assertTrue("이어진 곡이 재생되지 않는다", connection.playback.value.isPlaying)
        } finally {
            // 서비스 대기열은 다른 계측 테스트와 공유하므로 한 곡으로 되돌린다.
            instrumentation.runOnMainSync {
                connection.pause()
                connection.playTrack(MusicService.TEST_TONE_MEDIA_ID, "테스트 오디오", "Keuney Music")
            }
            withTimeoutOrNull(10_000) { connection.playback.first { it.queue.size == 1 } }
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
    }

    private suspend fun await(
        connection: PlayerConnection,
        timeoutMs: Long = 15_000,
        predicate: (PlaybackState) -> Boolean,
    ) = withTimeoutOrNull(timeoutMs) { connection.playback.first(predicate) }

    private fun testTone() = Track(
        id = MusicService.TEST_TONE_MEDIA_ID,
        title = "테스트 오디오",
        artist = "Keuney Music",
        artworkUrl = null,
        durationMs = null,
        source = SourceType.Remote,
    )
}
