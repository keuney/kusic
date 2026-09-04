package com.keuney.music.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keuney.music.core.player.RepeatMode
import com.keuney.music.core.settings.CacheLimit
import com.keuney.music.core.settings.ThemePreference
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultsToSystem() = runBlocking {
        withStore { store ->
            assertEquals(ThemePreference.System, DataStoreSettingsRepository(store).theme.first())
        }
    }

    @Test
    fun writesAndPersistsEveryThemeAcrossReopen() = runBlocking {
        for (theme in ThemePreference.entries) {
            withStore { store ->
                val repository = DataStoreSettingsRepository(store)
                repository.setTheme(theme)
                assertEquals(theme, repository.theme.first())
            }
            withStore { store ->
                assertEquals(theme, DataStoreSettingsRepository(store).theme.first())
            }
        }
    }

    @Test
    fun unknownStoredThemeFallsBackToSystem() = runBlocking {
        withStore { store ->
            store.edit { it[stringPreferencesKey("theme")] = "future-theme" }
            assertEquals(ThemePreference.System, DataStoreSettingsRepository(store).theme.first())
        }
    }

    @Test
    fun repeatDefaultsToOff() = runBlocking {
        withStore { store ->
            assertEquals(RepeatMode.Off, DataStoreSettingsRepository(store).repeatMode.first())
        }
    }

    @Test
    fun writesAndPersistsEveryRepeatModeAcrossReopen() = runBlocking {
        for (mode in RepeatMode.entries) {
            withStore { store ->
                val repository = DataStoreSettingsRepository(store)
                repository.setRepeatMode(mode)
                assertEquals(mode, repository.repeatMode.first())
            }
            // 앱 재시작에 해당한다. 저장소를 새로 열어도 남아야 한다.
            withStore { store ->
                assertEquals(mode, DataStoreSettingsRepository(store).repeatMode.first())
            }
        }
    }

    @Test
    fun unknownStoredRepeatModeFallsBackToOff() = runBlocking {
        withStore { store ->
            store.edit { it[stringPreferencesKey("repeat_mode")] = "future-mode" }
            assertEquals(RepeatMode.Off, DataStoreSettingsRepository(store).repeatMode.first())
        }
    }

    @Test
    fun historyIsOnUnlessTurnedOff() = runBlocking {
        // 켜져 있는 것이 기본이다. PRD 34가 최근 재생을 기능으로 두고 있다.
        withStore { store ->
            assertEquals(true, DataStoreSettingsRepository(store).historyEnabled.first())
        }
        withStore { store ->
            val repository = DataStoreSettingsRepository(store)
            repository.setHistoryEnabled(false)
            assertEquals(false, repository.historyEnabled.first())
        }
    }

    @Test
    fun historySettingSurvivesReopen() = runBlocking {
        withStore { store -> DataStoreSettingsRepository(store).setHistoryEnabled(false) }
        withStore { store ->
            assertEquals(false, DataStoreSettingsRepository(store).historyEnabled.first())
        }
    }

    @Test
    fun cacheLimitDefaultsToTwoHundredFiftySixMegabytes() = runBlocking {
        withStore { store ->
            assertEquals(CacheLimit.Mb256, DataStoreSettingsRepository(store).cacheLimit.first())
        }
    }

    @Test
    fun writesAndPersistsEveryCacheLimitAcrossReopen() = runBlocking {
        for (limit in CacheLimit.entries) {
            withStore { store ->
                val repository = DataStoreSettingsRepository(store)
                repository.setCacheLimit(limit)
                assertEquals(limit, repository.cacheLimit.first())
            }
            withStore { store ->
                assertEquals(limit, DataStoreSettingsRepository(store).cacheLimit.first())
            }
        }
    }

    @Test
    fun unknownStoredCacheLimitFallsBackToTheDefault() = runBlocking {
        // 이름이 사라진 뒤에도 재생은 되어야 한다.
        withStore { store ->
            store.edit { it[stringPreferencesKey("cache_limit")] = "Mb2048" }
            assertEquals(CacheLimit.Mb256, DataStoreSettingsRepository(store).cacheLimit.first())
        }
    }

    private suspend fun withStore(block: suspend (DataStore<Preferences>) -> Unit) {
        val job = SupervisorJob()
        val store = createSettingsDataStore(
            file = File(temporaryFolder.root, "settings.preferences_pb"),
            scope = CoroutineScope(job + Dispatchers.IO),
        )
        try {
            block(store)
        } finally {
            job.cancelAndJoin()
        }
    }
}
