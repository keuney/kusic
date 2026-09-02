package com.keuney.music.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
