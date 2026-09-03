package com.keuney.music.core.player

import android.content.ComponentName
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MusicSessionTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun controllerConnectsToServiceSessionAndCanReadPlayer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        try {
            val controller = future.get(10, TimeUnit.SECONDS)
            instrumentation.runOnMainSync {
                assertTrue(controller.isConnected)
                assertTrue(controller.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
                // 이전 재생이나 시스템 컨트롤러가 세션을 유지할 수 있으므로 상태를 직접 준비한다.
                controller.stop()
                assertEquals(Player.STATE_IDLE, controller.playbackState)
                assertEquals(1, controller.mediaItemCount)
                assertEquals("known-test-tone", controller.currentMediaItem?.mediaId)
            }
        } finally {
            instrumentation.runOnMainSync { MediaController.releaseFuture(future) }
        }
    }
}
