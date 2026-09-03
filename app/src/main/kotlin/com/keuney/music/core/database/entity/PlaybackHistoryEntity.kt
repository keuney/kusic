package com.keuney.music.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 재생 기록. 같은 곡을 여러 번 들으면 여러 행이 된다. 중복 정책은 기록을 읽는 쪽(KM-115)에서 정한다.
 */
@Entity(
    tableName = "playback_history",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("track_id"), Index("played_at")],
)
internal data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "track_id")
    val trackId: String,
    /** 들은 시각(epoch millis). 최근 재생을 뽑는 기준이다. */
    @ColumnInfo(name = "played_at")
    val playedAt: Long,
)
