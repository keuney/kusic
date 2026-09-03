package com.keuney.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.keuney.music.core.database.entity.PlaybackHistoryEntity
import com.keuney.music.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PlaybackHistoryDao {
    /**
     * 최근에 들은 것부터, 같은 곡은 한 번만.
     *
     * 표에는 들을 때마다 행이 쌓이므로 곡별로 가장 최근 시각만 남겨 묶는다. 같은 곡이 목록에
     * 여러 번 나오면 "최근 재생"이 쓸모없어진다(KM-115의 중복 정책).
     */
    @Query(
        "SELECT tracks.* FROM tracks INNER JOIN (" +
            "SELECT track_id, max(played_at) AS last_played FROM playback_history GROUP BY track_id" +
            ") AS recent ON tracks.id = recent.track_id " +
            "ORDER BY recent.last_played DESC LIMIT :limit",
    )
    fun observeRecent(limit: Int): Flow<List<TrackEntity>>

    /**
     * 한 곡을 다시 들으면 새 행을 쌓지 않고 그 곡의 기록을 갈아 끼운다(KM-115 중복 정책).
     *
     * 목록에는 곡별로 한 번만 나오므로 여러 행을 쌓아도 보이는 것은 같은데 표만 계속 자란다.
     * 곡마다 한 행이면 표 크기가 들은 곡 수를 넘지 않는다. 지우기와 삽입은 한 덩어리여야 한다.
     */
    @Transaction
    suspend fun record(entry: PlaybackHistoryEntity) {
        deleteFor(entry.trackId)
        insert(entry)
    }

    @Query("DELETE FROM playback_history WHERE track_id = :trackId")
    suspend fun deleteFor(trackId: String)

    @Insert
    suspend fun insert(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clear()
}
