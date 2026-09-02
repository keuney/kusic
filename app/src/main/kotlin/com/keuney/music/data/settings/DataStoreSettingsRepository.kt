package com.keuney.music.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    private companion object {
        val ThemeKey = stringPreferencesKey("theme")
        val WifiOnlyKey = booleanPreferencesKey("wifi_only_playback")
    }
}
