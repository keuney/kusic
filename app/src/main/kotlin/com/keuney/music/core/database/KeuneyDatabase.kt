package com.keuney.music.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.keuney.music.core.database.entity.FavoriteEntity
import com.keuney.music.core.database.entity.PlaybackHistoryEntity
import com.keuney.music.core.database.entity.PlaylistEntity
import com.keuney.music.core.database.entity.PlaylistItemEntity
import com.keuney.music.core.database.entity.TrackEntity

/**
 * 로컬 라이브러리 저장소. 스키마는 app/schemas로 내보내며 버전마다 파일이 남는다.
 *
 * 최근 검색어는 여기 없다. 개수가 정해진 짧은 문자열 목록이라 DataStore에 둔다(ADR-046).
 * DAO는 KM-111에서 추가한다. 이 작업은 표와 마이그레이션까지다.
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
abstract class KeuneyDatabase : RoomDatabase()
