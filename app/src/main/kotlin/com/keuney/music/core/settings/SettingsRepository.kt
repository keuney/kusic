package com.keuney.music.core.settings

import com.keuney.music.core.player.RepeatMode
import kotlinx.coroutines.flow.Flow

enum class ThemePreference { System, Light, Dark }

/**
 * 재생 캐시의 상한. 고를 수 있는 값만 둔다.
 *
 * 저장되는 것은 상수 이름이므로 이름을 바꾸면 이전에 저장된 설정을 읽지 못한다. 크기를 뜻하는
 * 이름을 쓰는 이유는 나중에 바이트 값을 손볼 때 저장된 값이 그대로 유효하도록 하기 위해서다.
 */
enum class CacheLimit(val bytes: Long) {
    Mb128(128L * 1024 * 1024),
    Mb256(256L * 1024 * 1024),
    Mb512(512L * 1024 * 1024),
}

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

    /**
     * 들은 곡을 최근 재생에 남길지. 기본값은 켜짐이다(PRD 34).
     *
     * 끄면 그때부터 남기지 않는다. 이미 남은 기록은 지우지 않는다. 지우는 것은 라이브러리의
     * 지우기가 하는 일이고, 설정을 끄는 것과 기록을 버리는 것은 다른 뜻이다.
     */
    val historyEnabled: Flow<Boolean>
    suspend fun setHistoryEnabled(enabled: Boolean)

    /**
     * 재생 캐시의 상한. 기본값은 [CacheLimit.Mb256]이다.
     *
     * 바뀐 값은 캐시를 만들 때 적용되므로 앱을 다시 시작한 뒤부터 유효하다. 쓰던 캐시의 상한을
     * 도중에 바꿀 방법이 Media3에 없다.
     */
    val cacheLimit: Flow<CacheLimit>
    suspend fun setCacheLimit(limit: CacheLimit)
}
