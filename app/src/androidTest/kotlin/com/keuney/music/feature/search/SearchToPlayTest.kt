package com.keuney.music.feature.search

import android.content.Intent
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.MainActivity
import com.keuney.music.core.player.ConnectionState
import com.keuney.music.core.player.MusicService
import com.keuney.music.core.player.NetworkPolicy
import com.keuney.music.core.player.PlaybackPhase
import com.keuney.music.core.player.PlayerConnection
import com.keuney.music.core.player.RepeatMode
import com.keuney.music.core.search.SearchHistoryRepository
import com.keuney.music.core.search.SearchRepository
import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.core.settings.ThemePreference
import com.keuney.music.feature.player.PlayerViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-058 인수 조건의 기기 검증: 실제 검색 → 결과 선택 → 재생 → Home 후 유지.
 * 검색 상태 전이 자체는 SearchViewModelTest가 일반 단위 검사로 다룬다.
 */
@HiltAndroidTest
class SearchToPlayTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var searchRepository: SearchRepository

    @Inject
    lateinit var searchHistoryRepository: SearchHistoryRepository

    @Test
    fun realQueryResultStartsPlaybackAndKeepsPlayingAfterHome(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val connection = PlayerConnection(context)
        val player = PlayerViewModel(connection, FakeSettings(), NetworkPolicy(FakeSettings()) { false })
        val search = SearchViewModel(searchRepository, searchHistoryRepository)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }

            search.search(QUERY)
            val results = withTimeout(30_000) {
                search.state.first { it is SearchUiState.Success } as SearchUiState.Success
            }
            assertTrue("검색 결과가 비어 있음", results.tracks.isNotEmpty())

            instrumentation.runOnMainSync { player.playTrack(results.tracks.first()) }
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
                connection.playTrack(MusicService.TEST_TONE_MEDIA_ID, "테스트 오디오", "Keuney Music")
            }
            runCatching { withTimeout(10_000) { connection.playback.first { it.durationMs > 100_000 } } }
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
    }

    private class FakeSettings : SettingsRepository {
        override val theme: Flow<ThemePreference> = MutableStateFlow(ThemePreference.System)
        override suspend fun setTheme(theme: ThemePreference) = Unit
        override val wifiOnlyPlayback: Flow<Boolean> = MutableStateFlow(false)
        override suspend fun setWifiOnlyPlayback(enabled: Boolean) = Unit
        override val repeatMode: Flow<RepeatMode> = MutableStateFlow(RepeatMode.Off)
        override suspend fun setRepeatMode(mode: RepeatMode) = Unit
    }

    private companion object {
        const val QUERY = "아이유"
    }
}
