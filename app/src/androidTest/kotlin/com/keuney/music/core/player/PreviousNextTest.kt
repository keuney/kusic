package com.keuney.music.core.player

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.MainActivity
import com.keuney.music.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-094 인수 조건: 화면 버튼과 알림 버튼이 같은 명령을 쓰고 같은 결과를 낸다.
 *
 * 대기열에 곡을 여러 개 넣는 경로는 KM-097 소속이라 여기서는 한 곡 상태를 다룬다. 그 상태에서
 * 이전은 곡의 처음으로 되돌리고 다음은 쓸 수 없어야 하며, 세 곳 모두 그렇게 보여야 한다.
 */
@HiltAndroidTest
class PreviousNextTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun previousRestartsTheTrackAndNextIsUnavailableEverywhere(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val connection = PlayerConnection(context)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync { connection.play() }
            withTimeout(15_000) { connection.playback.first { it.isPlaying && it.positionMs > 500 } }

            // 화면이 보는 가용성.
            val state = connection.playback.value
            assertTrue("한 곡뿐이어도 이전은 쓸 수 있어야 한다", state.hasPrevious)
            assertFalse("다음 곡이 없는데 다음이 쓸 수 있다고 나온다", state.hasNext)

            // 화면 버튼: 이전은 곡의 처음으로 되돌린다.
            instrumentation.runOnMainSync { connection.seekTo(40_000) }
            withTimeout(5_000) { connection.playback.first { it.positionMs > 39_000 } }
            instrumentation.runOnMainSync { connection.seekToPrevious() }
            withTimeout(5_000) { connection.playback.first { it.positionMs < 3_000 } }

            // 화면 버튼: 다음은 명령이 없어 아무 일도 하지 않는다.
            instrumentation.runOnMainSync { connection.seekTo(40_000) }
            withTimeout(5_000) { connection.playback.first { it.positionMs > 39_000 } }
            val beforeNext = connection.playback.value.positionMs
            instrumentation.runOnMainSync { connection.seekToNext() }
            delay(1_000)
            assertTrue(
                "다음이 재생 위치를 되돌렸다: $beforeNext → ${connection.playback.value.positionMs}",
                connection.playback.value.positionMs >= beforeNext,
            )

            // 알림: 이전 버튼은 있고 다음 버튼은 없다.
            val previousLabel =
                context.getString(androidx.media3.session.R.string.media3_controls_seek_to_previous_description)
            val nextLabel =
                context.getString(androidx.media3.session.R.string.media3_controls_seek_to_next_description)
            withTimeout(20_000) {
                while (ownActions(manager, context).none { it.title.toString() == previousLabel }) delay(200)
            }
            assertFalse(
                "다음 곡이 없는데 알림에 다음 버튼이 있다",
                ownActions(manager, context).any { it.title.toString() == nextLabel },
            )

            // 알림 버튼도 같은 결과를 낸다.
            instrumentation.runOnMainSync { connection.seekTo(40_000) }
            withTimeout(5_000) { connection.playback.first { it.positionMs > 39_000 } }
            ownActions(manager, context).first { it.title.toString() == previousLabel }.actionIntent.send()
            withTimeout(5_000) { connection.playback.first { it.positionMs < 3_000 } }
        } finally {
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
                activity.finish()
            }
        }
    }

    /** 이 앱의 재생 알림만 본다. 다른 앱 알림의 버튼을 누르지 않는다. */
    private fun ownActions(manager: NotificationManager, context: android.content.Context) =
        manager.activeNotifications
            .filter {
                it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ==
                    context.getString(R.string.test_audio)
            }
            .flatMap { it.notification.actions?.toList().orEmpty() }
}
