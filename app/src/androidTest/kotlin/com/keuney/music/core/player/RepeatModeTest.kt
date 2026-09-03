package com.keuney.music.core.player

import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.core.settings.SettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * KM-096 인수 조건: 세 반복 모드가 저장된 설정에서 재생까지 닿고 화면이 볼 상태로 돌아온다.
 *
 * 화면은 설정만 바꾸고 플레이어에 직접 지시하지 않으므로(ADR-054) 이 검사도 설정을 바꾸고
 * 상태를 본다. 저장 자체의 유지는 DataStoreSettingsRepositoryTest가 일반 단위 검사로 다룬다.
 */
@HiltAndroidTest
class RepeatModeTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var settings: SettingsRepository

    @Test
    fun everyRepeatModeReachesPlaybackFromTheStoredSetting(): Unit = runBlocking {
        hilt.inject()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connection = PlayerConnection(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync { connection.connect() }
            assertNotNull(
                "세션에 연결되지 않았다",
                withTimeoutOrNull(15_000) { connection.state.first { it == ConnectionState.Connected } },
            )

            // 세 모드를 모두 지난다. 마지막에 Off로 돌아오므로 기기 설정이 원래대로 남는다.
            listOf(RepeatMode.All, RepeatMode.One, RepeatMode.Off).forEach { mode ->
                settings.setRepeatMode(mode)
                val reached = withTimeoutOrNull(15_000) {
                    connection.playback.first { it.repeatMode == mode }
                }
                assertNotNull("설정한 반복 모드가 재생 상태로 오지 않았다: $mode", reached)
                assertEquals(mode, connection.playback.value.repeatMode)
            }
        } finally {
            settings.setRepeatMode(RepeatMode.Off)
            instrumentation.runOnMainSync { connection.disconnect() }
        }
    }
}
