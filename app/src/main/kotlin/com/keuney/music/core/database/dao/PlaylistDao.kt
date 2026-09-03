package com.keuney.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.keuney.music.core.database.entity.PlaylistItemEntity
import com.keuney.music.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

/** 재생목록과 목록의 곡 수를 함께 읽기 위한 결과 형태. */
internal data class PlaylistWithCount(
    val id: Long,
    val name: String,
    @androidx.room.ColumnInfo(name = "track_count")
    val trackCount: Int,
)

@Dao
internal interface PlaylistDao {
    @Query(
        "SELECT playlists.id, playlists.name, " +
            "(SELECT count(*) FROM playlist_items WHERE playlist_id = playlists.id) AS track_count " +
            "FROM playlists ORDER BY playlists.created_at DESC",
    )
    fun observeAll(): Flow<List<PlaylistWithCount>>

    /** 담은 자리 순서대로. 자리 번호가 같은 행은 나중에 담은 것이 뒤에 온다. */
    @Query(
        "SELECT tracks.* FROM tracks " +
            "INNER JOIN playlist_items ON tracks.id = playlist_items.track_id " +
            "WHERE playlist_items.playlist_id = :playlistId " +
            "ORDER BY playlist_items.position ASC, playlist_items.id ASC",
    )
    fun observeTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query("INSERT INTO playlists (name, created_at) VALUES (:name, :createdAt)")
    suspend fun create(name: String, createdAt: Long): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun rename(playlistId: Long, name: String)

    /** 담긴 항목은 외래 키가 함께 지운다. */
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun delete(playlistId: Long)

    @Insert
    suspend fun addItem(item: PlaylistItemEntity)

    @Query("SELECT coalesce(max(position), -1) + 1 FROM playlist_items WHERE playlist_id = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    /** 자리 번호를 정하고 담는 것은 한 덩어리여야 한다. 둘로 나뉘면 같은 자리가 두 번 나온다. */
    @Transaction
    suspend fun addTrack(playlistId: Long, trackId: String) {
        addItem(PlaylistItemEntity(playlistId = playlistId, trackId = trackId, position = nextPosition(playlistId)))
    }

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId AND track_id = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: String)
}
