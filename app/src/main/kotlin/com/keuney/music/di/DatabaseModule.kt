package com.keuney.music.di

import android.content.Context
import androidx.room.Room
import com.keuney.music.core.database.KEUNEY_MIGRATIONS
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
        Room.databaseBuilder(context, KeuneyDatabase::class.java, "keuney.db")
            // 지우고 다시 만들지 않는다. 사이드로드로 쓰는 앱이라 기기의 데이터가 유일한 사본이다.
            .addMigrations(*KEUNEY_MIGRATIONS)
            .build()
}
