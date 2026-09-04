package com.keuney.music

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import com.keuney.music.core.player.ConnectionState
import com.keuney.music.core.player.MusicService
import com.keuney.music.feature.player.PlayerViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * KM-133 인수 조건: Activity를 다시 만들어도 재생이 흔들리지 않는다.
 *
 * 화면 회전·다크 모드·글꼴 크기·창 크기 변경은 모두 같은 길로 온다. Activity가 파괴되고 다시
 * 만들어지며 ViewModel만 남는다. 회전 자체를 흉내 내지 않고 그 길을 직접 지나가게 한다.
 *
 * Activity 종료 뒤에도 재생이 이어지는 것은 BackgroundPlaybackTest가 다룬다. 여기서는 떠나지
 * 않고 다시 만들어질 때만 본다.
 */
@HiltAndroidTest
class ActivityRecreationTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun playbackAndTheSessionConnectionSurviveActivityRecreation(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // ViewModelProvider가 요구하는 것은 ViewModelStoreOwner다. startActivitySync는 Activity로
        // 돌려주므로 실제 타입으로 받는다.
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity
        // 화면이 실제로 쓰는 ViewModel이다. 검사용 컨트롤러를 따로 만들면 화면이 세션과 어떻게
        // 지내는지가 아니라 검사가 세션과 어떻게 지내는지를 보게 된다.
        lateinit var viewModel: PlayerViewModel
        instrumentation.runOnMainSync {
            viewModel = ViewModelProvider(activity)[PlayerViewModel::class.java]
        }
        try {
            assertNotNull(
                "세션에 연결되지 않았다",
                withTimeoutOrNull(15_000) {
                    viewModel.connectionState.first { it == ConnectionState.Connected }
                },
            )
            // 남아 있던 대기열에 기대지 않는다. 내장 음원이라 네트워크도 필요 없다.
            instrumentation.runOnMainSync { viewModel.playTrack(testTone()) }
            assertNotNull(
                "재생이 시작되지 않았다",
                withTimeoutOrNull(20_000) {
                    viewModel.playbackState.first { it.isPlaying && it.positionMs > 500 }
                },
            )
            val before = viewModel.playbackState.value.positionMs

            // 재생성 동안 연결이 어떻게 움직였는지 지켜본다. 끝난 뒤의 값만 보면 잠깐 끊겼다
            // 다시 붙은 것을 알아챌 수 없다.
            val seen = Collections.synchronizedList(mutableListOf<ConnectionState>())
            val watch = launch(Dispatchers.Main.immediate) {
                viewModel.connectionState.collect { seen += it }
            }

            instrumentation.runOnMainSync { activity.recreate() }
            assertNotNull(
                "Activity가 다시 만들어지지 않았다",
                withTimeoutOrNull(15_000) {
                    while (!destroyed(activity)) delay(50)
                    true
                },
            )
            // 새 Activity가 자리 잡고 재생이 더 나아갈 시간을 준다.
            delay(2_000)
            watch.cancel()

            assertEquals(
                "구성 변경만으로 세션 연결이 끊겼다",
                listOf(ConnectionState.Connected),
                seen.distinct(),
            )
            val state = viewModel.playbackState.value
            assertTrue("재생이 멈췄다", state.isPlaying)
            assertTrue("재생 위치가 나아가지 않았다", state.positionMs > before)
            assertEquals("들고 있던 곡이 바뀌었다", MusicService.TEST_TONE_MEDIA_ID, state.nowPlaying?.mediaId)
        } finally {
            instrumentation.runOnMainSync {
                viewModel.pause()
                viewModel.seekTo(0)
                // 다시 만들어진 뒤의 Activity를 닫는다. 처음 것은 이미 사라졌다.
                current()?.finish()
            }
        }
    }

    private fun destroyed(activity: Activity): Boolean {
        var value = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value = activity.isDestroyed }
        return value
    }

    /** 지금 살아 있는 이 앱의 Activity. 재생성 뒤에는 처음 인스턴스가 아니다. */
    private fun current(): Activity? = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED)
        .firstOrNull { it is MainActivity }

    private fun testTone() = Track(
        id = MusicService.TEST_TONE_MEDIA_ID,
        title = "테스트 오디오",
        artist = "Keuney Music",
        artworkUrl = null,
        durationMs = null,
        source = SourceType.Remote,
    )
}
