package com.keuney.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.keuney.music.core.settings.ThemePreference

/**
 * Material 3 기본 색을 그대로 쓴다. 이 앱은 고유한 브랜드 색을 정한 적이 없고, 기본 배색은
 * 밝을 때와 어두울 때의 대비가 이미 맞춰져 있다. 색을 직접 고르면 그 대비를 우리가 지켜야 한다.
 */
private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
internal fun KeuneyMusicTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/**
 * 저장된 설정과 시스템 설정을 합쳐 지금 어두운 화면인지 정한다.
 *
 * 시스템을 따르는 것이 기본값이다. 사용자가 밝게·어둡게를 고르면 시스템 설정과 달라도 그것을
 * 따른다. 고르지 않은 사람에게는 기기 설정이 이미 답이고, 고른 사람에게는 그 선택이 답이다.
 */
internal fun ThemePreference.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemePreference.System -> systemDark
    ThemePreference.Light -> false
    ThemePreference.Dark -> true
}
