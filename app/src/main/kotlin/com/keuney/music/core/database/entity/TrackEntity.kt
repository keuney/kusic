package com.keuney.music.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 로컬에 남기는 곡 메타데이터. 즐겨찾기·재생목록·재생 기록이 모두 이 표를 가리킨다.
 *
 * 재생 주소는 담지 않는다(AGENTS.md 8). 여기 있는 것은 표시용 값과 공급자가 준 ID뿐이며 재생할
 * 때마다 주소를 다시 해석한다. [artworkUrl]은 앨범 이미지 주소이고 재생 주소가 아니다.
 */
@Entity(tableName = "tracks")
internal data class TrackEntity(
    /** 공급자가 준 Track ID. 임의로 다시 만들지 않는다. */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "artist")
    val artist: String,
    @ColumnInfo(name = "artwork_url")
    val artworkUrl: String?,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long?,
    /** [com.keuney.music.core.model.SourceType]의 이름. 상수 이름을 바꾸면 이전 행을 읽지 못한다. */
    @ColumnInfo(name = "source")
    val source: String,
)
