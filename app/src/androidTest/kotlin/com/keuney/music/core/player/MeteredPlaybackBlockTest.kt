package com.keuney.music.core.player

import android.net.Uri
import androidx.media3.datasource.DataSpec
import com.keuney.music.core.model.PlayableStream
import com.keuney.music.core.model.Track
import com.keuney.music.core.settings.CacheLimit
import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.core.settings.ThemePreference
import com.keuney.music.data.source.MusicSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * KM-137: 제한이 걸리면 스트림 주소를 해석하기 전에 막고, 자리표시 URI가 아닌 요청은 건드리지 않는다.
 * 캐시 적중은 이 지점까지 오지 않으므로 제한과 무관하게 재생된다.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class MeteredPlaybackBlockTest {
    @Test
    fun blockedPolicyStopsTheRequestBeforeResolvingAStream() {
        val source = CountingSource()
        val resolver = TrackStreamResolver(StreamResolver(source), policy(wifiOnly = true, metered = true))

        try {
            resolver.resolveDataSpec(spec(TrackUri.of("gdZLi9oWNZg")))
            fail("측정 요금제에서 제한이 켜져 있으면 막아야 한다")
        } catch (expected: MeteredNetworkBlockedException) {
            assertEquals("Metered network playback is disabled", expected.message)
        }
        assertEquals("막힌 요청은 공급자를 호출하면 안 된다", 0, source.calls)
    }

    @Test
    fun unmeteredConnectionResolvesTheStreamEvenWithTheSettingOn() {
        val source = CountingSource()
        val resolver = TrackStreamResolver(StreamResolver(source), policy(wifiOnly = true, metered = false))

        val resolved = resolver.resolveDataSpec(spec(TrackUri.of("gdZLi9oWNZg")))

        assertEquals(1, source.calls)
        assertTrue("해석된 주소로 바뀌어야 한다", resolved.uri.toString().startsWith("https://"))
    }

    @Test
    fun requestsThatAreNotTrackPlaceholdersPassThroughUnchanged() {
        val source = CountingSource()
        val resolver = TrackStreamResolver(StreamResolver(source), policy(wifiOnly = true, metered = true))
        val local = spec("android.resource://com.keuney.music/1")

        val resolved = resolver.resolveDataSpec(local)

        assertEquals(local.uri, resolved.uri)
        assertEquals(0, source.calls)
    }

    private fun spec(uri: String) = DataSpec.Builder().setUri(Uri.parse(uri)).build()

    private fun policy(wifiOnly: Boolean, metered: Boolean) =
        NetworkPolicy(FakeSettings(wifiOnly)) { metered }

    private class CountingSource : MusicSource {
        var calls = 0
            private set

        override suspend fun search(query: String): Result<List<Track>> = Result.success(emptyList())
        override suspend fun getTrack(trackId: String): Result<Track> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getRelated(trackId: String): Result<List<Track>> =
            Result.failure(UnsupportedOperationException())

        override suspend fun resolveStream(trackId: String): Result<PlayableStream> {
            calls++
            return Result.success(PlayableStream("https://example.invalid/progressive", "video/mp4", 306098))
        }
    }

    private class FakeSettings(private val wifiOnly: Boolean) : SettingsRepository {
        override val theme: Flow<ThemePreference> = MutableStateFlow(ThemePreference.System)
        override suspend fun setTheme(theme: ThemePreference) = Unit
        override val wifiOnlyPlayback: Flow<Boolean> = MutableStateFlow(wifiOnly)
        override suspend fun setWifiOnlyPlayback(enabled: Boolean) = Unit
        override val repeatMode: Flow<RepeatMode> = MutableStateFlow(RepeatMode.Off)
        override suspend fun setRepeatMode(mode: RepeatMode) = Unit
        override val historyEnabled: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setHistoryEnabled(enabled: Boolean) = Unit
        override val cacheLimit: Flow<CacheLimit> = MutableStateFlow(CacheLimit.Mb256)
        override suspend fun setCacheLimit(limit: CacheLimit) = Unit
    }
}
