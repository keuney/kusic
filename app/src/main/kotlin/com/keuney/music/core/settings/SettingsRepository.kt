package com.keuney.music.core.settings

import com.keuney.music.core.player.RepeatMode
import kotlinx.coroutines.flow.Flow

enum class ThemePreference { System, Light, Dark }

interface SettingsRepository {
    val theme: Flow<ThemePreference>
    suspend fun setTheme(theme: ThemePreference)

    /** 켜면 측정 요금제 연결에서는 새로 내려받는 재생을 시작하지 않는다. 기본값은 꺼짐이다. */
    val wifiOnlyPlayback: Flow<Boolean>
    suspend fun setWifiOnlyPlayback(enabled: Boolean)

    /**
     * 반복 모드. 앱을 다시 켜도 유지된다(PRD 34).
     *
     * 저장된 값이 곧 재생에 적용되는 값이다. 화면은 이 값을 바꾸기만 하고 플레이어에 직접
     * 지시하지 않는다. 적용은 재생을 소유한 MusicService가 한다.
     */
    val repeatMode: Flow<RepeatMode>
    suspend fun setRepeatMode(mode: RepeatMode)
}
