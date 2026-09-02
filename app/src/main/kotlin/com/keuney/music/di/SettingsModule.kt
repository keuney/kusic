package com.keuney.music.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.data.settings.DataStoreSettingsRepository
import com.keuney.music.data.settings.createSettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsModule {
    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> =
        createSettingsDataStore(File(context.filesDir, "datastore/settings.preferences_pb"))

    @Provides
    @Singleton
    fun provideSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository = repository
}
