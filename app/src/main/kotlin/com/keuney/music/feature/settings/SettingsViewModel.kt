package com.keuney.music.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keuney.music.core.player.PlaybackCache
import com.keuney.music.core.settings.CacheLimit
import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.core.settings.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 설정 화면이 보여주고 바꾸는 것 전부.
 *
 * 재생 중에 곧바로 적용되는 설정(반복)은 [com.keuney.music.feature.player.PlayerViewModel]이
 * 재생 화면에서 다룬다. 여기 있는 것은 앱 전체에 걸리거나 다음 재생부터 적용되는 것들이다.
 */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val playbackCache: PlaybackCache,
) : ViewModel() {
    /**
     * 구독을 기다리지 않고 곧바로 읽는다. 화면 색은 첫 화면부터 필요하기 때문이다.
     *
     * 그래도 저장소에서 값이 오기 전의 첫 프레임은 기본값(시스템)으로 그린다. 어둡게를 골라
     * 둔 사람은 아주 짧게 시스템 색을 볼 수 있다. 이것을 없애려면 값이 올 때까지 화면을
     * 그리지 않아야 하는데, 그 대가로 시작이 늦어진다.
     */
    val theme: StateFlow<ThemePreference> = settings.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.System)

    val wifiOnlyPlayback: StateFlow<Boolean> = settings.wifiOnlyPlayback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val historyEnabled: StateFlow<Boolean> = settings.historyEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val cacheLimit: StateFlow<CacheLimit> = settings.cacheLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CacheLimit.Mb256)

    /** 지금 열려 있는 캐시에 걸린 상한. 설정을 바꿨어도 다시 시작하기 전에는 이 값이다. */
    val activeCacheLimitBytes: Long get() = playbackCache.limitBytes

    /**
     * 캐시가 쓰고 있는 크기. 흐름이 아니라 물어보는 값이다.
     *
     * Media3 캐시는 크기가 바뀌었다고 알려 주지 않는다. 재생 중에는 계속 늘어나므로 계속
     * 지켜본다면 화면이 쉬지 않고 다시 그려진다. 화면에 들어올 때와 비운 뒤에만 읽는다.
     */
    private val mutableCacheUsedBytes = MutableStateFlow(0L)
    val cacheUsedBytes: StateFlow<Long> = mutableCacheUsedBytes.asStateFlow()

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun setWifiOnlyPlayback(enabled: Boolean) {
        viewModelScope.launch { settings.setWifiOnlyPlayback(enabled) }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setHistoryEnabled(enabled) }
    }

    fun setCacheLimit(limit: CacheLimit) {
        viewModelScope.launch { settings.setCacheLimit(limit) }
    }

    fun refreshCacheUsage() {
        // 파일을 세는 일이라 다른 스레드에서 한다.
        viewModelScope.launch {
            mutableCacheUsedBytes.value = withContext(Dispatchers.IO) { playbackCache.usedBytes }
        }
    }

    /** 보관 중인 구간을 비운다. 다시 들으면 다시 받는다. 재생 중인 구간은 남을 수 있다. */
    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { playbackCache.clear() }
            mutableCacheUsedBytes.value = withContext(Dispatchers.IO) { playbackCache.usedBytes }
        }
    }
}
