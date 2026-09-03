package com.keuney.music.core.model

/** 사용자가 만든 재생목록. 담긴 곡은 따로 읽는다. 목록 화면에서 곡까지 다 들고 있을 필요가 없다. */
data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
)
