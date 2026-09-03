package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.core.library.LibraryRepository
import com.keuney.music.core.model.Track
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * KM-115 인수 조건: 실제로 들은 곡만 기록에 남고, 목록에서 지울 수 있다.
 *
 * 기록은 재생을 소유한 MusicService가 남기므로 여기서는 재생만 시키고 저장소를 본다. 얼마나
 * 들으면 남기는지의 규칙은 PlaybackHistoryTest가 일반 단위 검사로 다룬다.
 */
@HiltAndroidTest
class PlaybackHistoryRecordingTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var library: LibraryRepository

    @Test
    fun listeningLongEnoughLeavesARecordAndItCanBeCleared(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            library.clearPlaybackHistory()

            instrumentation.runOnMainSync { connection.connect() }
            assertNotNull(
                "세션에 연결되지 않았다",
                withTimeoutOrNull(15_000) { connection.state.first { it == ConnectionState.Connected } },
            )

            // 잠깐 듣고 멈춘 곡은 남지 않는다. 훑어보며 넘긴 곡까지 남으면 최근 재생이 쓸모없다.
            instrumentation.runOnMainSync { connection.play() }
            assertNotNull(
                "재생이 시작되지 않았다",
                withTimeoutOrNull(20_000) {
                    connection.playback.first { it.isPlaying && it.positionMs > 500 }
                },
            )
            instrumentation.runOnMainSync { connection.pause() }
            delay(4_000)
            assertEquals(
                "기준보다 짧게 들었는데 기록이 남았다",
                emptyList<Track>(),
                library.recentlyPlayed(10).first(),
            )

            // 기준을 넘겨 들으면 남는다. 위치를 앞으로 옮겨 기다리는 시간을 줄인다.
            instrumentation.runOnMainSync {
                connection.seekTo(8_000)
                connection.play()
            }
            assertNotNull(
                "기준을 넘겨 들었는데 기록이 남지 않았다",
                withTimeoutOrNull(30_000) { awaitRecord(MusicService.TEST_TONE_MEDIA_ID) },
            )

            // 지우면 비워진다. 지우기는 이 검사가 직접 하므로 곧바로 보인다.
            library.clearPlaybackHistory()
            assertEquals(emptyList<Track>(), library.recentlyPlayed(10).first())
        } finally {
            library.clearPlaybackHistory()
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
    }

    /**
     * 기록이 남을 때까지 다시 물어본다. 관찰이 아니라 물어보는 이유가 있다.
     *
     * Hilt는 계측 검사마다 새 싱글턴 컴포넌트를 만들므로 이 검사가 받은 저장소는 서비스가 쓰는
     * 것과 **다른 인스턴스**다. 데이터베이스 파일은 같아 다시 물어보면 보이지만, Room의 변경
     * 알림은 자기 인스턴스의 쓰기만 알기 때문에 관찰만으로는 서비스가 남긴 기록이 오지 않는다.
     * 실제 앱은 컴포넌트가 하나이므로 화면은 관찰만으로 갱신된다.
     */
    private suspend fun awaitRecord(trackId: String): List<Track> {
        while (true) {
            val tracks = library.recentlyPlayed(10).first()
            if (tracks.any { it.id == trackId }) return tracks
            delay(500)
        }
    }
}
