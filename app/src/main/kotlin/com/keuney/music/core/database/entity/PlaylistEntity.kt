package com.keuney.music.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 사용자가 만든 재생목록. 이름은 중복을 막지 않는다. 같은 이름을 쓰는 것은 사용자의 선택이다. */
@Entity(tableName = "playlists")
internal data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    /** 만든 시각(epoch millis). */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
