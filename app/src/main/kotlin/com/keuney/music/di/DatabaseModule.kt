package com.keuney.music.di

import android.content.Context
import androidx.room.Room
import com.keuney.music.core.database.KeuneyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KeuneyDatabase =
        Room.databaseBuilder(context, KeuneyDatabase::class.java, "keuney.db").build()
}
