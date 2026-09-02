package com.keuney.music.core.player

import android.content.Intent
import android.os.PowerManager
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ScreenOffPlaybackTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun keepsPlayingWithTheScreenOffForSixtySeconds(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val power = context.getSystemService(PowerManager::class.java)
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val connection = PlayerConnection(context)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync { connection.play() }
            withTimeout(15_000) { connection.playback.first { it.phase == PlaybackPhase.Playing } }
            instrumentation.runOnMainSync { connection.seekTo(0) }
            withTimeout(5_000) { connection.playback.first { it.positionMs < 1000 } }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_SLEEP)
            withTimeout(5_000) { while (power.isInteractive) delay(50) }
            instrumentation.runOnMainSync {
                activity.finish()
                connection.disconnect()
            }
            repeat(62) {
                delay(1000)
                assertFalse("화면이 검사 도중 켜짐", power.isInteractive)
            }
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            withTimeout(5_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs >= 60_000 }
            }
            assertFalse("재생 확인 시 화면이 켜짐", power.isInteractive)
        } finally {
            instrumentation.runOnMainSync {
                activity.finish()
                connection.pause()
            }
            if (connection.state.value == ConnectionState.Connected) {
                withTimeout(5_000) { connection.playback.first { !it.playWhenReady } }
            }
            instrumentation.runOnMainSync {
                connection.disconnect()
                context.stopService(Intent(context, MusicService::class.java))
            }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_WAKEUP)
        }
    }
}
