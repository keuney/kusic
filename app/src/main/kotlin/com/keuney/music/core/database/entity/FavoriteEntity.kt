package com.keuney.music.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 즐겨찾기. 곡마다 한 행뿐이므로 곡 ID가 그대로 기본 키다.
 *
 * 곡이 지워지면 함께 지운다. 가리키는 곡이 없는 즐겨찾기는 화면에 그릴 수 없다.
 */
@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "track_id")
    val trackId: String,
    /** 추가한 시각(epoch millis). 최근에 추가한 것부터 보여주는 데 쓴다. */
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
)
