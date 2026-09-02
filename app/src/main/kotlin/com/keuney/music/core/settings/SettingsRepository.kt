package com.keuney.music.core.settings

import kotlinx.coroutines.flow.Flow

enum class ThemePreference { System, Light, Dark }

interface SettingsRepository {
    val theme: Flow<ThemePreference>
    suspend fun setTheme(theme: ThemePreference)

    /** 켜면 측정 요금제 연결에서는 새로 내려받는 재생을 시작하지 않는다. 기본값은 꺼짐이다. */
    val wifiOnlyPlayback: Flow<Boolean>
    suspend fun setWifiOnlyPlayback(enabled: Boolean)
}
