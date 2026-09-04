package com.keuney.music.ui.theme

import com.keuney.music.core.settings.ThemePreference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KM-152 인수 조건: 시스템·밝게·어둡게 세 가지가 있고 고른 값이 화면 색을 정한다.
 *
 * 색 자체는 Material 3 기본 배색을 그대로 쓰므로 여기서 검사할 것이 없다. 검사할 것은 저장된
 * 설정과 기기 설정을 합쳐 어느 쪽을 그릴지 정하는 규칙이다.
 */
class ThemeTest {
    @Test
    fun systemFollowsTheDevice() {
        assertTrue(ThemePreference.System.isDark(systemDark = true))
        assertFalse(ThemePreference.System.isDark(systemDark = false))
    }

    @Test
    fun aChosenValueWinsOverTheDevice() {
        // 고른 사람에게는 그 선택이 답이다. 기기 설정과 달라도 따른다.
        assertFalse(ThemePreference.Light.isDark(systemDark = true))
        assertTrue(ThemePreference.Dark.isDark(systemDark = false))
    }

    @Test
    fun aChosenValueAgreesWithTheDeviceWhenTheyMatch() {
        assertTrue(ThemePreference.Dark.isDark(systemDark = true))
        assertFalse(ThemePreference.Light.isDark(systemDark = false))
    }
}
