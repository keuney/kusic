package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestAudioPlaybackTest {
    @Test
    fun testAudioPlaysPausesSeeksAndResumes(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync { connection.play() }
            withTimeout(15_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 500 }
            }
            assertTrue(connection.playback.value.durationMs in 119_000..121_000)
            instrumentation.runOnMainSync { connection.pause() }
            withTimeout(5_000) { connection.playback.first { it.phase == PlaybackPhase.Paused } }
            delay(300)
            val pausedPosition = connection.playback.value.positionMs
            delay(500)
            assertEquals(pausedPosition, connection.playback.value.positionMs)
            instrumentation.runOnMainSync { connection.seekTo(30_000) }
            withTimeout(5_000) { connection.playback.first { it.positionMs in 29_900..30_100 } }
            instrumentation.runOnMainSync { connection.play() }
            withTimeout(5_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 30_500 }
            }
        } finally {
            instrumentation.runOnMainSync {
                connection.pause()
                connection.disconnect()
            }
        }
    }
}
