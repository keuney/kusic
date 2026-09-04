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
 * KM-138 인수 조건: 재생할 수 없는 곡에서 대기열이 멈춰 서지 않는다.
 *
 * 없는 Track ID를 첫 곡으로 넣는다. 공급자가 그 ID를 거부하므로 해석이 실패하고, 그것이 곧
 * "곡을 가져올 수 없다"([PlaybackFailure.Source])다. 실패를 일부러 만들 수 있는 유일한 방법이며
 * 네트워크를 끊는 것과 달리 공급자 응답에 기대므로 결과가 늘 같다.
 *
 * 무엇을 넘기고 무엇을 넘기지 않는지의 규칙은 UnplayableSkipTest가 일반 단위 검사로 다룬다.
 */
@HiltAndroidTest
class SkipUnplayableTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun anUnplayableTrackIsSkippedAndTheNextOnePlays(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            assertNotNull(
                "세션에 연결되지 않았다",
                withTimeoutOrNull(15_000) { connection.state.first { it == ConnectionState.Connected } },
            )

            // 첫 곡은 재생할 수 없고 두 번째는 내장 음원이다. 검색 결과에서 고른 것과 같은 경로다.
            instrumentation.runOnMainSync {
                connection.playQueue(listOf(unplayable(), testTone()), startIndex = 0)
            }

            // 첫 곡에서 멈추지 않고 두 번째 곡이 재생된다. 해석 실패와 한 번의 재시도까지
            // 기다려야 하므로 넉넉히 준다.
            assertNotNull(
                "재생할 수 없는 곡에서 대기열이 멈춰 섰다",
                withTimeoutOrNull(60_000) {
                    connection.playback.first { it.queueIndex == 1 && it.isPlaying }
                },
            )

            // 무엇이 빠졌는지 화면이 말할 수 있어야 한다.
            assertEquals(
                "지나간 곡이 상태에 남지 않았다",
                UNPLAYABLE_TITLE,
                connection.playback.value.skippedTitle,
            )

            // 사용자가 무엇을 재생할지 바꾸면 안내는 지워진다.
            instrumentation.runOnMainSync { connection.seekToQueueItem(1) }
            assertNotNull(
                "조작한 뒤에도 안내가 남아 있다",
                withTimeoutOrNull(10_000) { connection.playback.first { it.skippedTitle == null } },
            )
        } finally {
            // 서비스 대기열은 다른 계측 검사와 공유하므로 한 곡으로 되돌린다.
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

    private fun unplayable() = Track(
        id = "km138-not-a-real-track-id",
        title = UNPLAYABLE_TITLE,
        artist = "Keuney Music",
        artworkUrl = null,
        durationMs = null,
        source = SourceType.Remote,
    )

    private fun testTone() = Track(
        id = MusicService.TEST_TONE_MEDIA_ID,
        title = "테스트 오디오",
        artist = "Keuney Music",
        artworkUrl = null,
        durationMs = null,
        source = SourceType.Remote,
    )

    private companion object {
        const val UNPLAYABLE_TITLE = "없는 곡"
    }
}
