package com.keuney.music.core.player

import android.app.Instrumentation
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import androidx.test.platform.app.InstrumentationRegistry
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
 * KM-095 인수 조건: 토글이 세션까지 닿고 대기열이 그대로 유지된다.
 *
 * 대기열을 화면에서 만드는 경로는 KM-097 소속이라 여기서는 `playQueue`로 직접 넣는다. 이 검사가
 * 보는 것은 대기열뿐이며 각 항목의 스트림 해석이나 재생 성공은 대상이 아니다. 주소 해석은 항목을
 * 열 때 일어나므로 대기열 확인에는 영향이 없다.
 *
 * **셔플을 켠 상태에서는 다음 곡으로 넘어가는 순서를 확인하지 않는다.** 세션이 컨트롤러에 보내는
 * `Timeline`에 셔플 순서가 실려 오지 않아(`RemotableTimeline`은 선형 순서로 되돌아간다) 컨트롤러가
 * 스스로 답할 때는 넣은 순서가 나오고, 세션이 진짜 상태를 밀어 넣은 뒤에는 섞인 순서가 나온다.
 * 어느 쪽이 보일지는 시점에 달려 있어 순회 결과를 고정할 수 없다. 실제로 10번 다시 섞어도 매번 넣은
 * 순서였던 실행과, 넣은 순서대로 가다 중간에서 더 못 나아간 실행을 모두 관찰했다.
 *
 * 그래서 켠 상태는 순회 대신 대기열 자체(`PlaybackState.queue`)로 본다. 그 목록은 Timeline의 창
 * 순서, 즉 넣은 순서이며 셔플과 무관하게 결정적이다. 섞인 재생 순서는 세션 뒤 Media3의 동작이며
 * UI 계층에서 관찰할 수 없다. 이 한계는 ADR-053에 적었고 대기열 화면(KM-097)에도 같은 제약이다.
 */
@HiltAndroidTest
class ShuffleTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun theShuffleToggleReachesTheSessionAndKeepsTheQueue(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            assertNotNull(
                "세션에 연결되지 않았다",
                withTimeoutOrNull(15_000) { connection.state.first { it == ConnectionState.Connected } },
            )

            // 앞선 계측이 남긴 대기열·재생 상태와 무관하게 시작한다.
            resetQueue(instrumentation, connection, shuffle = false)

            // 토글이 상태로 돌아온다. 화면의 켜짐 표시가 근거로 삼는 값이다.
            instrumentation.runOnMainSync { connection.setShuffleEnabled(true) }
            assertNotNull(
                "셔플을 켰는데 상태에 반영되지 않았다",
                withTimeoutOrNull(15_000) { connection.playback.first { it.shuffleEnabled } },
            )
            instrumentation.runOnMainSync { connection.setShuffleEnabled(false) }
            assertNotNull(
                "셔플을 껐는데 상태에 반영되지 않았다",
                withTimeoutOrNull(15_000) { connection.playback.first { !it.shuffleEnabled } },
            )

            // 셔플이 꺼져 있으면 다음 곡으로 넘어가는 순서가 넣은 순서다.
            assertEquals(
                "셔플이 꺼졌는데 넣은 순서로 넘어가지 않는다",
                QUEUE_IDS,
                visitQueueWithoutShuffle(instrumentation, connection),
            )

            // 셔플을 켜도 대기열 목록은 넣은 순서 그대로다. 섞인 순서는 세션 뒤에 있다.
            resetQueue(instrumentation, connection, shuffle = true)
            assertEquals(
                "셔플을 켜자 대기열 목록이 달라졌다",
                QUEUE_IDS,
                connection.playback.value.queue.map(NowPlaying::mediaId),
            )
        } finally {
            // 서비스 대기열과 셔플 설정은 다른 계측 테스트와 공유하므로 되돌린다.
            instrumentation.runOnMainSync {
                connection.setShuffleEnabled(false)
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

    /** 대기열을 처음부터 다시 넣는다. */
    private suspend fun resetQueue(
        instrumentation: Instrumentation,
        connection: PlayerConnection,
        shuffle: Boolean,
    ) {
        instrumentation.runOnMainSync {
            connection.setShuffleEnabled(false)
            connection.playQueue(QUEUE_IDS.map(::queueTrack))
            // 소리를 낼 필요가 없다. 대기열만 본다.
            connection.pause()
            connection.setShuffleEnabled(shuffle)
        }
        assertNotNull(
            "대기열을 넣었는데 첫 곡이 현재 곡이 되지 않았다(셔플=$shuffle)",
            withTimeoutOrNull(15_000) {
                connection.playback.first {
                    it.nowPlaying?.mediaId == QUEUE_IDS.first() && it.shuffleEnabled == shuffle
                }
            },
        )
    }

    /**
     * 셔플이 꺼진 대기열을 다시 넣고 끝까지 지나가며 만난 곡의 ID를 모은다. 셔플을 켠 상태에서는
     * 순회 결과가 결정적이지 않으므로 이 방법을 쓰지 않는다.
     */
    private suspend fun visitQueueWithoutShuffle(
        instrumentation: Instrumentation,
        connection: PlayerConnection,
    ): List<String> {
        resetQueue(instrumentation, connection, shuffle = false)
        val visited = mutableListOf(QUEUE_IDS.first())
        repeat(QUEUE_IDS.size - 1) {
            val current = visited.last()
            val next = advanceOrNull(instrumentation, connection, current)
            assertNotNull("$current 다음으로 넘어가지 않았다", next)
            visited += next.orEmpty()
        }
        return visited
    }

    /** 다음으로 넘어가고 상태가 바뀔 때까지 기다린다. 컨트롤러 명령은 비동기다. */
    private suspend fun advanceOrNull(
        instrumentation: Instrumentation,
        connection: PlayerConnection,
        current: String,
    ): String? {
        instrumentation.runOnMainSync { connection.seekToNext() }
        return withTimeoutOrNull(8_000) {
            connection.playback.first { state ->
                state.nowPlaying?.mediaId?.let { it != current } == true
            }
        }?.nowPlaying?.mediaId
    }

    private companion object {
        val QUEUE_IDS = listOf("queue-order-1", "queue-order-2", "queue-order-3", "queue-order-4")

        fun queueTrack(id: String) =
            Track(id, "대기열 순서 확인 $id", "Keuney Music", null, null, SourceType.Remote)
    }
}
