package com.keuney.music.core.player

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.MainActivity
import com.keuney.music.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MediaNotificationTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun notificationShowsMetadataAndItsActionsControlPlayback(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(context, MusicService::class.java)),
        ).buildAsync()
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            val controller = future.get(10, TimeUnit.SECONDS)
            instrumentation.runOnMainSync {
                assertNotNull(controller.mediaMetadata.artworkData)
                controller.prepare()
                controller.play()
            }
            withTimeout(15_000) {
                while (manager.activeNotifications.none {
                        it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ==
                            context.getString(R.string.test_audio) && it.notification.getLargeIcon() != null
                    }) delay(100)
            }
            val notification = manager.activeNotifications.first {
                it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ==
                    context.getString(R.string.test_audio)
            }.notification
            assertEquals(context.getString(R.string.app_name), notification.extras.getCharSequence(Notification.EXTRA_TEXT))
            assertNotNull(notification.contentIntent)
            val pauseLabel = context.getString(androidx.media3.session.R.string.media3_controls_pause_description)
            val playLabel = context.getString(androidx.media3.session.R.string.media3_controls_play_description)
            withTimeout(5_000) {
                while (manager.activeNotifications.none { item ->
                        item.notification.actions?.any { it.title.toString() == pauseLabel } == true
                    }) delay(100)
            }
            manager.activeNotifications.flatMap { it.notification.actions?.toList().orEmpty() }
                .first { it.title.toString() == pauseLabel }.actionIntent.send()
            withTimeout(5_000) {
                while (true) {
                    var paused = false
                    instrumentation.runOnMainSync { paused = !controller.playWhenReady }
                    if (paused) break
                    delay(50)
                }
            }
            withTimeout(5_000) {
                while (manager.activeNotifications.none { item ->
                        item.notification.actions?.any { it.title.toString() == playLabel } == true
                    }) delay(100)
            }
            manager.activeNotifications.flatMap { it.notification.actions?.toList().orEmpty() }
                .first { it.title.toString() == playLabel }.actionIntent.send()
            withTimeout(5_000) {
                while (true) {
                    var playing = false
                    instrumentation.runOnMainSync { playing = controller.isPlaying }
                    if (playing) break
                    delay(50)
                }
            }
            instrumentation.runOnMainSync {
                assertTrue(controller.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS))
                controller.pause()
            }
        } finally {
            instrumentation.runOnMainSync {
                activity.finish()
                MediaController.releaseFuture(future)
                context.stopService(Intent(context, MusicService::class.java))
            }
        }
    }
}
