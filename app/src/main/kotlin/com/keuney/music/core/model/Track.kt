package com.keuney.music.core.model

/** Metadata only; a resolved playback URL must never be part of a track. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationMs: Long?,
    val source: SourceType,
)

/** Describes the content origin independently of the adapter used to access it. */
enum class SourceType {
    Remote,
}
