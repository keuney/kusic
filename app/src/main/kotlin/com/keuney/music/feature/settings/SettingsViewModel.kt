package com.keuney.music.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.core.settings.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 앱 전체에 걸리는 설정. 지금은 화면 색뿐이고 설정 화면(KM-153)이 나머지를 여기에 더한다.
 *
 * 재생과 관련된 설정(WiFi 전용 재생·반복)은 [com.keuney.music.feature.player.PlayerViewModel]이
 * 들고 있다. 그것들은 재생 화면에서 바꾸고 재생에 곧바로 적용된다.
 */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
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

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { settings.setTheme(theme) }
    }
}
