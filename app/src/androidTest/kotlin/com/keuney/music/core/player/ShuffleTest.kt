package com.keuney.music.core.player

import android.app.Instrumentation
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
 * **섞인 재생 순서는 이 자리에서 확인할 수 없다.** 세션이 컨트롤러에 보내는 `Timeline`에는 셔플
 * 순서가 실려 오지 않아(`RemotableTimeline`은 선형 순서로 되돌아간다) 컨트롤러 쪽에서 다음 곡을
 * 물으면 언제나 넣은 순서가 나온다. 실제로 10번 다시 섞어 확인했더니 매번 넣은 순서였고, 반대로
 * 세션이 진짜 상태를 밀어 넣는 순간에는 컨트롤러가 다음이 있다고 본 항목으로 넘어가지 못했다.
 * 즉 섞인 순서는 세션 뒤 Media3의 동작이며 UI 계층에서 관찰할 수 없다. 이 한계는 ADR-053에 적었고
 * 대기열 화면(KM-097)에도 같은 제약이 적용된다.
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

            // 대기열은 넣은 순서 그대로 남아 있다.
            assertEquals(
                "셔플이 꺼진 대기열이 넣은 순서가 아니다",
                QUEUE_IDS,
                visitQueue(instrumentation, connection, shuffle = false),
            )

            // 셔플을 켜도 대기열 내용과 컨트롤러가 보는 순서는 그대로다. 섞인 순서는 세션 뒤에 있다.
            assertEquals(
                "셔플을 켜자 대기열 내용이 달라졌다",
                QUEUE_IDS,
                visitQueue(instrumentation, connection, shuffle = true),
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
            connection.playQueue(QUEUE_IDS.map { Triple(it, "대기열 순서 확인 $it", "Keuney Music") })
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

    /** 대기열을 다시 넣고 끝까지 지나가며 만난 곡의 ID를 모은다. */
    private suspend fun visitQueue(
        instrumentation: Instrumentation,
        connection: PlayerConnection,
        shuffle: Boolean,
    ): List<String> {
        resetQueue(instrumentation, connection, shuffle)
        val visited = mutableListOf(QUEUE_IDS.first())
        repeat(QUEUE_IDS.size - 1) {
            val current = visited.last()
            val next = advanceOrNull(instrumentation, connection, current)
            assertNotNull("$current 다음으로 넘어가지 않았다(셔플=$shuffle)", next)
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
    }
}
