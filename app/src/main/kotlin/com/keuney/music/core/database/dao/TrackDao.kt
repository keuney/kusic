package com.keuney.music.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.keuney.music.core.database.entity.TrackEntity

@Dao
internal interface TrackDao {
    /**
     * 곡 메타데이터를 넣거나 갱신한다. 즐겨찾기·재생목록·재생 기록이 모두 이 표를 가리키므로
     * 그것들을 쓰기 전에 곡이 먼저 있어야 한다.
     */
    @Upsert
    suspend fun upsert(track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun find(trackId: String): TrackEntity?

    /** 어디에서도 가리키지 않는 곡을 지운다. 남겨 두면 표가 계속 자란다. */
    @Query(
        "DELETE FROM tracks WHERE id NOT IN (SELECT track_id FROM favorites) " +
            "AND id NOT IN (SELECT track_id FROM playlist_items) " +
            "AND id NOT IN (SELECT track_id FROM playback_history)",
    )
    suspend fun deleteUnreferenced()
}
