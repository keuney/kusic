package com.keuney.music.core.model

import java.time.Instant

/**
 * Ephemeral playback metadata. Never persist this object or its URL.
 * [requestHeaders] must be sent with the playback request; some sources reject a stream
 * fetched with a different client identity than the one that resolved it.
 */
data class PlayableStream(
    val url: String,
    val mimeType: String?,
    val bitrate: Int?,
    val expiresAt: Instant? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
) {
    override fun toString(): String = "PlayableStream(url=<redacted>)"
}
