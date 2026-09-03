package com.keuney.music.di

import android.content.Context
import androidx.room.Room
import com.keuney.music.core.database.KEUNEY_MIGRATIONS
import com.keuney.music.core.database.KeuneyDatabase
import com.keuney.music.core.database.dao.FavoriteDao
import com.keuney.music.core.database.dao.PlaybackHistoryDao
import com.keuney.music.core.database.dao.PlaylistDao
import com.keuney.music.core.database.dao.TrackDao
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

    // DAO는 저장소 구현만 받는다. 화면과 ViewModel의 주입 그래프에는 올리지 않는다(AGENTS.md 10).
    @Provides
    fun provideTrackDao(database: KeuneyDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideFavoriteDao(database: KeuneyDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun providePlaylistDao(database: KeuneyDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun providePlaybackHistoryDao(database: KeuneyDatabase): PlaybackHistoryDao =
        database.playbackHistoryDao()
}
