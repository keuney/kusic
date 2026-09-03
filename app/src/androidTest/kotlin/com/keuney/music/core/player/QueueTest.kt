package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * KM-097 인수 조건: 대기열 목록·현재 곡 표시·빼기·자리 옮기기가 화면이 보는 상태에 반영된다.
 *
 * 화면은 `PlaybackState.queue`와 `queueIndex`만 보고 그리므로 이 검사도 그 값을 본다. 각 항목의
 * 스트림 해석이나 재생 성공은 대상이 아니다. 주소 해석은 항목을 열 때 일어난다.
 */
@HiltAndroidTest
class QueueTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun theQueueIsListedAndCanBeChanged(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            assertNotNull(
                "세션에 연결되지 않았다",
                withTimeoutOrNull(15_000) { connection.state.first { it == ConnectionState.Connected } },
            )

            // 목록에서 두 번째 곡을 골랐을 때처럼 넣는다. 나머지는 대기열에 남아야 한다.
            instrumentation.runOnMainSync {
                connection.playQueue(QUEUE_IDS.map(::queueTrack), startIndex = 1)
                connection.pause()
            }
            assertNotNull(
                "대기열이 상태로 오지 않았다",
                awaitQueue(connection) { it.queue.map(NowPlaying::mediaId) == QUEUE_IDS },
            )
            assertEquals("고른 자리가 현재 곡이 아니다", 1, connection.playback.value.queueIndex)
            assertEquals(QUEUE_IDS[1], connection.playback.value.nowPlaying?.mediaId)

            // 제목도 함께 온다. 화면이 목록을 그릴 수 있어야 한다.
            assertEquals(
                QUEUE_IDS.map { "대기열 확인 $it" },
                connection.playback.value.queue.map(NowPlaying::title),
            )

            // 자리 옮기기: 첫 곡을 세 번째로 보낸다.
            instrumentation.runOnMainSync { connection.moveQueueItem(0, 2) }
            val moved = listOf(QUEUE_IDS[1], QUEUE_IDS[2], QUEUE_IDS[0], QUEUE_IDS[3])
            assertNotNull(
                "자리를 옮긴 결과가 상태로 오지 않았다",
                awaitQueue(connection) { it.queue.map(NowPlaying::mediaId) == moved },
            )
            // 옮겨도 재생 중인 곡은 그대로다. 자리만 앞으로 왔다.
            assertEquals(QUEUE_IDS[1], connection.playback.value.nowPlaying?.mediaId)
            assertEquals(0, connection.playback.value.queueIndex)

            // 빼기: 재생 중이 아닌 마지막 곡을 뺀다.
            instrumentation.runOnMainSync { connection.removeQueueItem(3) }
            val removed = moved.dropLast(1)
            assertNotNull(
                "뺀 결과가 상태로 오지 않았다",
                awaitQueue(connection) { it.queue.map(NowPlaying::mediaId) == removed },
            )
            assertEquals(QUEUE_IDS[1], connection.playback.value.nowPlaying?.mediaId)

            // 대기열 밖을 가리키는 요청은 아무 일도 하지 않는다.
            instrumentation.runOnMainSync {
                connection.removeQueueItem(99)
                connection.moveQueueItem(0, 99)
            }
            assertEquals(removed, connection.playback.value.queue.map(NowPlaying::mediaId))
        } finally {
            // 서비스 대기열은 다른 계측 테스트와 공유하므로 내장 테스트 음원으로 되돌린다.
            instrumentation.runOnMainSync {
                connection.pause()
                connection.playTrack(MusicService.TEST_TONE_MEDIA_ID, "테스트 오디오", "Keuney Music")
            }
            withTimeoutOrNull(10_000) { connection.playback.first { it.durationMs > 100_000 } }
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
    }

    private suspend fun awaitQueue(connection: PlayerConnection, predicate: (PlaybackState) -> Boolean) =
        withTimeoutOrNull(15_000) { connection.playback.first(predicate) }

    private companion object {
        val QUEUE_IDS = listOf("queue-ui-1", "queue-ui-2", "queue-ui-3", "queue-ui-4")

        fun queueTrack(id: String) =
            Track(id, "대기열 확인 $id", "Keuney Music", null, null, SourceType.Remote)
    }
}
