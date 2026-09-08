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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * KM-139 인수 조건: 목록을 무작위 순서로 재생하기 시작한다.
 *
 * 여기서 보는 것은 **세션에 실제로 들어간 대기열**이다. 그 목록(`PlaybackState.queue`)은 Timeline의
 * 창 순서이며 화면의 대기열이 보여 주는 것과 같은 값이다. 곡이 빠지거나 겹치지 않는지, 첫 곡이
 * 그 목록의 첫 줄인지, 셔플 모드가 꺼진 채로 시작하는지를 확인한다.
 *
 * 셔플 모드가 꺼져 있으면 다음 곡으로 넘어가는 순서가 대기열 목록의 순서라는 것은 ShuffleTest가
 * 이미 순회로 확인했다. 그래서 여기서는 그 순회를 되풀이하지 않고 모드가 꺼진 것만 본다. 두
 * 사실을 합치면 "보이는 순서가 재생될 순서"가 된다.
 *
 * 순서가 매번 다른지는 단위 검사(ShuffledQueueTest)가 씨앗을 고정해 확인한다. 실기기에서 몇 번
 * 눌러 서로 다른지를 보는 방식은 여섯 곡이면 우연히 같을 수 있어 판정이 흔들린다.
 *
 * 각 항목의 스트림 해석이나 재생 성공은 대상이 아니다. 대기열만 확인하므로 소리를 내지 않는다.
 */
@HiltAndroidTest
class ShufflePlayTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun shufflePlayFillsTheQueueWithEveryTrackInAnOrderThatWillActuallyPlay(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        val tracks = QUEUE_IDS.map(::shuffleTrack)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            assertNotNull(
                "세션에 연결되지 않았다",
                withTimeoutOrNull(15_000) { connection.state.first { it == ConnectionState.Connected } },
            )

            // 셔플 모드가 켜진 상태에서 시작한다. 꺼진 채로 시작하는 것이 이 기능의 규칙이므로
            // 그 일이 실제로 일어나는지 보려면 반대 상태에서 출발해야 한다.
            instrumentation.runOnMainSync { connection.setShuffleEnabled(true) }
            assertNotNull(
                "셔플을 켰는데 상태에 반영되지 않았다",
                withTimeoutOrNull(15_000) { connection.playback.first { it.shuffleEnabled } },
            )

            instrumentation.runOnMainSync {
                connection.shufflePlay(tracks)
                // 소리를 낼 필요가 없다. 대기열만 본다.
                connection.pause()
            }
            val state = withTimeoutOrNull(15_000) {
                connection.playback.first { it.queue.size == QUEUE_IDS.size }
            }
            assertNotNull("대기열이 목록 전체로 채워지지 않았다", state)
            val queue = connection.playback.value.queue.map(NowPlaying::mediaId)

            assertEquals("곡이 빠지거나 겹쳤다", QUEUE_IDS.toSet(), queue.toSet())
            assertEquals("대기열의 곡 수가 목록과 다르다", QUEUE_IDS.size, queue.size)
            assertEquals(
                "대기열의 첫 줄이 아닌 곡에서 시작했다",
                queue.first(),
                connection.playback.value.nowPlaying?.mediaId,
            )
            assertFalse(
                "셔플 모드가 켜진 채로 시작했다. 대기열에 보이는 순서가 실제 순서가 아니게 된다",
                connection.playback.value.shuffleEnabled,
            )

            // 빈 목록으로는 지금 듣고 있는 것을 밀어내지 않는다.
            instrumentation.runOnMainSync { connection.shufflePlay(emptyList()) }
            assertEquals(
                "빈 목록이 대기열을 지웠다",
                queue,
                connection.playback.value.queue.map(NowPlaying::mediaId),
            )
        } finally {
            // 서비스 대기열과 셔플 설정은 다른 계측 테스트와 공유하므로 되돌린다.
            instrumentation.runOnMainSync {
                connection.setShuffleEnabled(false)
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

    private fun shuffleTrack(id: String) =
        Track(id, "셔플 확인 $id", "Keuney Music", null, null, SourceType.Remote)

    private companion object {
        val QUEUE_IDS = listOf(
            "shuffle-play-1",
            "shuffle-play-2",
            "shuffle-play-3",
            "shuffle-play-4",
            "shuffle-play-5",
            "shuffle-play-6",
        )
    }
}
