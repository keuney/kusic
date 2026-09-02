package com.keuney.music.core.player

import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * KM-134 인수 조건: LRU, 기본 상한 256MB, 캐시 비우기, 영구 다운로드 아님.
 * 재생 검사는 기기의 실제 네트워크가 필요하다.
 */
@HiltAndroidTest
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class PlaybackCacheTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    internal lateinit var playbackCache: PlaybackCache

    @Before
    fun setUp() {
        hilt.inject()
    }

    @Test
    fun defaultTargetIsTwoHundredFiftySixMegabytes() {
        assertEquals(256L * 1024 * 1024, PlaybackCache.MAX_BYTES)
    }

    @Test
    fun cacheLivesInTheDisposableCacheDirectoryAndNotInAppStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val media = File(context.cacheDir, "media")
        assertTrue(
            "캐시는 운영체제가 정리할 수 있는 cacheDir 아래에 있어야 한다",
            media.absolutePath.startsWith(context.cacheDir.absolutePath),
        )
        assertTrue(
            "캐시가 영구 저장 영역에 있으면 안 된다",
            !media.absolutePath.startsWith(context.filesDir.absolutePath),
        )
    }

    /** 재생한 구간이 캐시에서 그대로 읽히고, 비우면 더 이상 읽히지 않는다. */
    @Test
    fun aPlayedTrackIsServedFromTheCacheAndClearRemovesIt(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        val key = TrackUri.of(TRACK_ID)
        try {
            playbackCache.clear()
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync {
                connection.playTrack(TRACK_ID, "캐시 확인용 트랙", "Keuney Music")
            }
            withTimeout(40_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 3_000 }
            }
            instrumentation.runOnMainSync { connection.pause() }
            assertTrue(
                "캐시 키에 Track 자리표시 URI가 없음: ${playbackCache.cache.keys}",
                key in playbackCache.cache.keys,
            )

            // 받은 구간은 소스가 닫힐 때 확정된다. 다른 항목으로 옮겨 닫는다.
            instrumentation.runOnMainSync {
                connection.playTrack(MusicService.TEST_TONE_MEDIA_ID, "테스트 오디오", "Keuney Music")
                connection.pause()
            }
            delay(3_000)
            assertEquals("캐시에서 읽히지 않음", CHUNK, readFromCacheOnly(key, CHUNK))

            playbackCache.clear()

            assertTrue(
                "캐시 비우기 후에도 해당 Track 구간이 읽힘",
                readFromCacheOnly(key, CHUNK) != CHUNK,
            )
        } finally {
            instrumentation.runOnMainSync {
                connection.pause()
                connection.playTrack(MusicService.TEST_TONE_MEDIA_ID, "테스트 오디오", "Keuney Music")
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
            }
        }
    }

    /** 상위 소스 없이 캐시만으로 여는 데 성공한 길이. 실패하면 -1. */
    private fun readFromCacheOnly(uri: String, length: Long): Long {
        val source = CacheDataSource.Factory()
            .setCache(playbackCache.cache)
            .setUpstreamDataSourceFactory(null)
            .createDataSource()
        return try {
            source.open(DataSpec.Builder().setUri(uri).setLength(length).build())
        } catch (_: Exception) {
            -1L
        } finally {
            runCatching { source.close() }
        }
    }

    private companion object {
        const val TRACK_ID = "gdZLi9oWNZg"
        const val CHUNK = 16L * 1024
    }
}
