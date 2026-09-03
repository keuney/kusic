package com.keuney.music.core.player

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import com.keuney.music.MainActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.core.search.SearchRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-091: 대기열 항목에 앨범 이미지 주소를 넣은 뒤에도 알림이 그대로 동작하는지 다시 확인한다.
 * KM-037·038은 이미지가 없는 상태(자리표시자)에서 검증했으므로 여기서 원격 곡으로 재확인한다.
 *
 * 실제 검색 응답과 이미지 CDN이 필요하다. 이미지를 받지 못하면 알림 큰 아이콘이 비어 실패한다.
 */
@HiltAndroidTest
class NowPlayingArtworkTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var searchRepository: SearchRepository

    @Test
    fun aRemoteTrackCarriesItsArtworkIntoTheSessionAndTheNotification(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val connection = PlayerConnection(context)
        try {
            val tracks = withTimeout(30_000) { searchRepository.search(QUERY).getOrThrow() }
            val track = tracks.firstOrNull { !it.artworkUrl.isNullOrBlank() }
            assertNotNull("이미지 주소가 있는 검색 결과가 없음", track)
            requireNotNull(track)
            assertTrue("공급자가 https 이미지 주소를 주지 않음", track.artworkUrl!!.startsWith("https://"))

            instrumentation.runOnMainSync { connection.connect() }
            withTimeout(10_000) { connection.state.first { it == ConnectionState.Connected } }
            instrumentation.runOnMainSync {
                connection.playTrack(track.id, track.title, track.artist, track.artworkUrl)
            }
            val playing = withTimeout(40_000) {
                connection.playback.first { it.isPlaying && it.positionMs > 1_000 }
            }

            // 화면이 쓸 상태에 이미지 주소가 그대로 돌아온다.
            assertEquals(track.artworkUrl, playing.nowPlaying?.artworkUri)
            assertEquals(track.id, playing.nowPlaying?.mediaId)
            assertEquals(track.title, playing.nowPlaying?.title)

            // 알림은 제목·아티스트와 함께 실제 이미지를 큰 아이콘으로 싣는다.
            withTimeout(30_000) {
                while (manager.activeNotifications.none { it.matches(track.title) }) delay(200)
            }
            val notification = manager.activeNotifications.first { it.matches(track.title) }.notification
            assertEquals(track.artist, notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
            assertNotNull("알림 PendingIntent 없음", notification.contentIntent)
            withTimeout(30_000) {
                while (manager.activeNotifications.none { it.matches(track.title) && it.notification.getLargeIcon() != null }) {
                    delay(200)
                }
            }
        } finally {
            // 서비스 대기열은 다른 계측 테스트와 공유하므로 내장 테스트 음원으로 되돌린다.
            instrumentation.runOnMainSync {
                connection.pause()
                connection.playTrack(MusicService.TEST_TONE_MEDIA_ID, "테스트 오디오", "Keuney Music")
            }
            runCatching { withTimeout(10_000) { connection.playback.first { it.durationMs > 100_000 } } }
            instrumentation.runOnMainSync {
                connection.pause()
                connection.seekTo(0)
                connection.disconnect()
                activity.finish()
            }
        }
    }

    private fun android.service.notification.StatusBarNotification.matches(title: String): Boolean =
        notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() == title

    private companion object {
        const val QUERY = "아이유"
    }
}
