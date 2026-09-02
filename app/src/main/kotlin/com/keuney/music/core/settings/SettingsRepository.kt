package com.keuney.music.core.settings

import kotlinx.coroutines.flow.Flow

enum class ThemePreference { System, Light, Dark }

interface SettingsRepository {
    val theme: Flow<ThemePreference>
    suspend fun setTheme(theme: ThemePreference)
}
