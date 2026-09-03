package com.keuney.music.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keuney.music.core.player.RepeatMode
import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.core.settings.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val theme: Flow<ThemePreference> = dataStore.data.map { preferences ->
        ThemePreference.entries.firstOrNull { it.name == preferences[ThemeKey] }
            ?: ThemePreference.System
    }.distinctUntilChanged()

    override suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[ThemeKey] = theme.name }
    }

    override val wifiOnlyPlayback: Flow<Boolean> = dataStore.data
        .map { it[WifiOnlyKey] ?: false }
        .distinctUntilChanged()

    override suspend fun setWifiOnlyPlayback(enabled: Boolean) {
        dataStore.edit { it[WifiOnlyKey] = enabled }
    }

    override val repeatMode: Flow<RepeatMode> = dataStore.data.map { preferences ->
        // 알 수 없는 값은 반복 없음으로 본다. 반복을 켠 것으로 잘못 보는 쪽이 더 나쁘다.
        RepeatMode.entries.firstOrNull { it.name == preferences[RepeatModeKey] } ?: RepeatMode.Off
    }.distinctUntilChanged()

    override suspend fun setRepeatMode(mode: RepeatMode) {
        dataStore.edit { it[RepeatModeKey] = mode.name }
    }

    private companion object {
        val ThemeKey = stringPreferencesKey("theme")
        val WifiOnlyKey = booleanPreferencesKey("wifi_only_playback")
        val RepeatModeKey = stringPreferencesKey("repeat_mode")
    }
}
