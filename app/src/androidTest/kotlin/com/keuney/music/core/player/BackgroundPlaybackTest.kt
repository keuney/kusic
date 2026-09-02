package com.keuney.music.core.player

import android.content.Intent
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class BackgroundPlaybackTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun keepsPlayingForThirtySecondsAfterHomeAndActivityDestruction(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val connection = PlayerConnection(context)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync { connection.play() }
            withTimeout(15_000) {
                connection.playback.first { it.phase == PlaybackPhase.Playing && it.positionMs > 500 }
            }
            val startPosition = connection.playback.value.positionMs
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_HOME)
            instrumentation.runOnMainSync {
                activity.finish()
                connection.disconnect()
            }
            withTimeout(5_000) {
                while (true) {
                    var destroyed = false
                    instrumentation.runOnMainSync { destroyed = activity.isDestroyed }
                    if (destroyed) break
                    delay(50)
                }
            }
            // 이 동안 UI 및 테스트 컨트롤러를 모두 해제하여 서비스만으로 재생하게 한다.
            delay(32_000)
            val serviceDump = readServiceDump()
            assertTrue("MusicService가 사라짐", serviceDump.contains("MusicService"))
            assertTrue("foreground 서비스가 아님", serviceDump.contains("isForeground=true"))
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            withTimeout(5_000) {
                connection.playback.first {
                    it.phase == PlaybackPhase.Playing && it.positionMs >= startPosition + 30_000
                }
            }
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
            instrumentation.waitForIdleSync()
        }
    }

    private fun readServiceDump(): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("dumpsys activity services com.keuney.music"),
    ).bufferedReader().use { it.readText() }
}
