package com.keuney.music.core.model

import java.time.Instant

/** Ephemeral playback metadata. Never persist this object or its URL. */
data class PlayableStream(
    val url: String,
    val mimeType: String?,
    val bitrate: Int?,
    val expiresAt: Instant? = null,
) {
    override fun toString(): String = "PlayableStream(url=<redacted>)"
}
