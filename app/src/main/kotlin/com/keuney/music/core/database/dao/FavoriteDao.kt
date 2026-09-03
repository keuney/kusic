package com.keuney.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keuney.music.core.database.entity.FavoriteEntity
import com.keuney.music.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FavoriteDao {
    /** 최근에 추가한 것부터. 화면이 다시 정렬하지 않도록 여기서 순서를 정한다. */
    @Query(
        "SELECT tracks.* FROM tracks INNER JOIN favorites ON tracks.id = favorites.track_id " +
            "ORDER BY favorites.added_at DESC",
    )
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE track_id = :trackId)")
    fun observeIsFavorite(trackId: String): Flow<Boolean>

    /** 같은 곡을 다시 추가하면 추가 시각만 갱신한다. 행이 두 개가 되지 않는다. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE track_id = :trackId")
    suspend fun remove(trackId: String)
}
