package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.data.source.MusicSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-059 Provider A Gate의 기기 판정. 실제 검색 결과로 큐 재생과 긴 곡 재생을 확인한다.
 * 기기의 실제 네트워크와 공급자 응답에 의존한다.
 */
@HiltAndroidTest
class ProviderAGateDeviceTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var source: MusicSource

    @Test
    fun aQueueAdvancesToTheNextTrackOnItsOwn(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            val tracks = source.search("BTS Dynamite").getOrThrow().take(2)
            assertTrue("큐 검증에 필요한 두 곡을 얻지 못함", tracks.size == 2)

            connect(connection)
            instrumentation.runOnMainSync {
                connection.playQueue(tracks.map { Triple(it.id, it.title, it.artist) })
            }
            withTimeout(60_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 1_000 }
            }
            var current: String? = null
            instrumentation.runOnMainSync { current = connection.currentMediaId() }
            assertTrue("첫 곡이 큐의 첫 항목이 아님", current == tracks[0].id)

            // 첫 곡 끝으로 이동해 자동 전환을 확인한다.
            val duration = connection.playback.value.durationMs
            assertTrue("첫 곡 길이를 얻지 못함", duration > 10_000)
            instrumentation.runOnMainSync { connection.seekTo(duration - 4_000) }

            withTimeout(60_000) {
                while (true) {
                    var id: String? = null
                    instrumentation.runOnMainSync { id = connection.currentMediaId() }
                    if (id == tracks[1].id) break
                    delay(500)
                }
            }
            withTimeout(40_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 1_000 }
            }
        } finally {
            restore(connection)
        }
    }

    @Test
    fun aLongTrackPlaysFromAFarPosition(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            val long = source.search("Beethoven Symphony No 9 full").getOrThrow()
                .filter { (it.durationMs ?: 0) >= 7 * 60 * 1000L }
                .maxByOrNull { it.durationMs ?: 0 }
            assertTrue("7분 이상 트랙을 찾지 못함", long != null)
            val track = requireNotNull(long)

            connect(connection)
            instrumentation.runOnMainSync { connection.playTrack(track.id, track.title, track.artist) }
            withTimeout(60_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 1_000 }
            }
            val duration = connection.playback.value.durationMs
            assertTrue("긴 곡 길이가 7분 미만: $duration", duration >= 7 * 60 * 1000L)

            // 파일 한참 뒤로 이동해 이어 재생되는지 본다.
            val target = duration - 120_000
            instrumentation.runOnMainSync { connection.seekTo(target) }
            withTimeout(60_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > target + 1_000 }
            }
            val seeked = connection.playback.value.positionMs
            delay(5_000)
            val advanced = connection.playback.value
            assertTrue(
                "긴 곡의 먼 지점에서 재생이 이어지지 않음: $seeked → ${advanced.positionMs}",
                advanced.positionMs > seeked + 3_000,
            )
            assertTrue("긴 곡 재생 중 오류 상태", advanced.phase == PlaybackPhase.Playing)
        } finally {
            restore(connection)
        }
    }

    private suspend fun connect(connection: PlayerConnection) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync { connection.connect() }
        withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
    }

    /** 서비스와 대기열은 다른 계측 테스트와 공유되므로 내장 테스트 음원으로 되돌린다. */
    private suspend fun restore(connection: PlayerConnection) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
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
