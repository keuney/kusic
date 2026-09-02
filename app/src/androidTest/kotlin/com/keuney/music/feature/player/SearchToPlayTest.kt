package com.keuney.music.feature.player

import android.content.Intent
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.MainActivity
import com.keuney.music.core.model.PlayableStream
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import com.keuney.music.core.player.ConnectionState
import com.keuney.music.core.player.PlaybackPhase
import com.keuney.music.core.player.PlayerConnection
import com.keuney.music.data.source.MusicSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-058 인수 조건: query 입력 → results 표시 → result 선택 → playback 시작 → Home 후 유지.
 * 마지막 검사는 기기의 실제 네트워크와 공급자 응답에 의존한다.
 */
@HiltAndroidTest
class SearchToPlayTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var source: MusicSource

    @Test
    fun searchStateMovesFromSearchingToResults(): Unit = runBlocking {
        val viewModel = viewModelWith(FakeSource(Result.success(listOf(track("a"), track("b")))))

        assertEquals(SearchUiState.Idle, viewModel.searchState.value)
        viewModel.search("아이유")
        val results = withTimeout(5_000) {
            viewModel.searchState.first { it is SearchUiState.Results } as SearchUiState.Results
        }

        assertEquals(listOf("a", "b"), results.tracks.map(Track::id))
    }

    @Test
    fun emptyResultsAndFailuresAreShownWithoutRawErrors(): Unit = runBlocking {
        val empty = viewModelWith(FakeSource(Result.success(emptyList())))
        empty.search("결과 없는 검색어")
        assertEquals(SearchUiState.Empty, withTimeout(5_000) { empty.searchState.first { it != SearchUiState.Searching && it != SearchUiState.Idle } })

        val failing = viewModelWith(FakeSource(Result.failure(IllegalStateException("원문 노출 금지"))))
        failing.search("실패하는 검색어")
        assertEquals(SearchUiState.Failed, withTimeout(5_000) { failing.searchState.first { it != SearchUiState.Searching && it != SearchUiState.Idle } })
    }

    @Test
    fun blankQueryReturnsToIdleWithoutCallingTheSource(): Unit = runBlocking {
        val fake = FakeSource(Result.success(listOf(track("a"))))
        val viewModel = viewModelWith(fake)

        viewModel.search("   ")

        assertEquals(SearchUiState.Idle, viewModel.searchState.value)
        assertEquals(0, fake.calls)
    }

    @Test
    fun realQueryResultStartsPlaybackAndKeepsPlayingAfterHome(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val connection = PlayerConnection(context)
        val viewModel = PlayerViewModel(connection, source)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }

            viewModel.search(QUERY)
            val results = withTimeout(30_000) {
                viewModel.searchState.first { it is SearchUiState.Results } as SearchUiState.Results
            }
            assertTrue("검색 결과가 비어 있음", results.tracks.isNotEmpty())

            instrumentation.runOnMainSync { viewModel.playTrack(results.tracks.first()) }
            withTimeout(40_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 1_000 }
            }

            val beforeHome = connection.playback.value.positionMs
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_HOME)
            instrumentation.runOnMainSync { activity.finish() }
            delay(5_000)
            val afterHome = connection.playback.value
            assertTrue(
                "Home 이후 재생이 유지되지 않음: $beforeHome → ${afterHome.positionMs}",
                afterHome.positionMs > beforeHome + 3_000,
            )
            assertEquals(PlaybackPhase.Playing, afterHome.phase)
        } finally {
            // 서비스 대기열은 다른 계측 테스트와 공유하므로 내장 테스트 음원으로 되돌린다.
            instrumentation.runOnMainSync {
                connection.pause()
                connection.playTrack(TEST_TONE_MEDIA_ID, "테스트 오디오", "Keuney Music")
            }
            runCatching { withTimeout(10_000) { connection.playback.first { it.durationMs > 100_000 } } }
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
    }

    private fun viewModelWith(fake: MusicSource) =
        PlayerViewModel(PlayerConnection(InstrumentationRegistry.getInstrumentation().targetContext), fake)

    private fun track(id: String) = Track(id, "제목 $id", "아티스트", null, 180_000, SourceType.Remote)

    private class FakeSource(private val result: Result<List<Track>>) : MusicSource {
        var calls = 0
            private set

        override suspend fun search(query: String): Result<List<Track>> {
            calls++
            return result
        }

        override suspend fun getTrack(trackId: String): Result<Track> = Result.failure(UnsupportedOperationException())
        override suspend fun resolveStream(trackId: String): Result<PlayableStream> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getRelated(trackId: String): Result<List<Track>> =
            Result.failure(UnsupportedOperationException())
    }

    private companion object {
        const val QUERY = "아이유"
        const val TEST_TONE_MEDIA_ID = "known-test-tone"
    }
}
