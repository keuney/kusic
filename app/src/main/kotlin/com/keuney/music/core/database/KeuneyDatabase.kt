package com.keuney.music.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.keuney.music.core.database.dao.FavoriteDao
import com.keuney.music.core.database.dao.PlaybackHistoryDao
import com.keuney.music.core.database.dao.PlaylistDao
import com.keuney.music.core.database.dao.TrackDao
import com.keuney.music.core.database.entity.FavoriteEntity
import com.keuney.music.core.database.entity.PlaybackHistoryEntity
import com.keuney.music.core.database.entity.PlaylistEntity
import com.keuney.music.core.database.entity.PlaylistItemEntity
import com.keuney.music.core.database.entity.TrackEntity

/**
 * 로컬 라이브러리 저장소. 스키마는 app/schemas로 내보내며 버전마다 파일이 남는다.
 *
 * 최근 검색어는 여기 없다. 개수가 정해진 짧은 문자열 목록이라 DataStore에 둔다(ADR-046).
 *
 * DAO는 이 클래스 밖으로 나가지 않는다. 화면과 ViewModel은 LibraryRepository만 본다(AGENTS.md 10).
 */
@Database(
    entities = [
        TrackEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PlaybackHistoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class KeuneyDatabase : RoomDatabase() {
    internal abstract fun trackDao(): TrackDao
    internal abstract fun favoriteDao(): FavoriteDao
    internal abstract fun playlistDao(): PlaylistDao
    internal abstract fun playbackHistoryDao(): PlaybackHistoryDao
}
