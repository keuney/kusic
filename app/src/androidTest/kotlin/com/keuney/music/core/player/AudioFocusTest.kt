package com.keuney.music.core.player

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AudioFocusTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun transientLossPausesAndResumesWhilePermanentLossStaysPaused(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(AudioManager::class.java)
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val connection = PlayerConnection(context)
        val transient = competingFocus(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        val permanent = competingFocus(AudioManager.AUDIOFOCUS_GAIN)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync { connection.play() }
            withTimeout(15_000) { connection.playback.first { it.phase == PlaybackPhase.Playing } }
            instrumentation.runOnMainSync {
                assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, manager.requestAudioFocus(transient))
            }
            withTimeout(5_000) { connection.playback.first { it.phase == PlaybackPhase.Paused } }
            delay(300)
            val pausedPosition = connection.playback.value.positionMs
            delay(500)
            assertEquals(pausedPosition, connection.playback.value.positionMs)
            manager.abandonAudioFocusRequest(transient)
            withTimeout(5_000) { connection.playback.first { it.phase == PlaybackPhase.Playing } }
            instrumentation.runOnMainSync {
                assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, manager.requestAudioFocus(permanent))
            }
            withTimeout(5_000) {
                connection.playback.first { it.phase == PlaybackPhase.Paused && !it.playWhenReady }
            }
            manager.abandonAudioFocusRequest(permanent)
            delay(500)
            assertFalse(connection.playback.value.playWhenReady)
        } finally {
            instrumentation.runOnMainSync {
                connection.pause()
                connection.disconnect()
                activity.finish()
                manager.abandonAudioFocusRequest(transient)
                manager.abandonAudioFocusRequest(permanent)
                context.stopService(Intent(context, MusicService::class.java))
            }
        }
    }

    private fun competingFocus(gain: Int): AudioFocusRequest = AudioFocusRequest.Builder(gain)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .setOnAudioFocusChangeListener({}, Handler(Looper.getMainLooper()))
        .build()
}
