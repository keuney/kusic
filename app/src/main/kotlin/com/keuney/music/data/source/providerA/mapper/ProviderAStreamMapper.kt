package com.keuney.music.data.source.providerA.mapper

import com.keuney.music.core.model.PlayableStream
import com.keuney.music.data.source.providerA.ProviderAStreamException
import java.net.URI
import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal fun mapStreamResponse(response: JsonObject, now: Instant): PlayableStream {
    val status = (response["playabilityStatus"] as? JsonObject)?.string("status")
    if (status != "OK") throw ProviderAStreamException("Track is not playable")
    val data = response["streamingData"] as? JsonObject
        ?: throw ProviderAStreamException("Streaming metadata missing")
    val formats = (data["adaptiveFormats"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    val audio = formats.filter {
        it.string("mimeType")?.substringBefore(';') in setOf("audio/mp4", "audio/webm") &&
            it.string("url")?.let(::isHttpsStream) == true
    }.maxByOrNull { (it["bitrate"] as? JsonPrimitive)?.intOrNull ?: 0 }
        ?: throw ProviderAStreamException("No direct audio stream available")
    val expirySeconds = (data["expiresInSeconds"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 }
    val expiry = expirySeconds?.let { runCatching { now.plusSeconds(it) }.getOrNull() }
    return PlayableStream(
        url = checkNotNull(audio.string("url")),
        mimeType = audio.string("mimeType")?.substringBefore(';'),
        bitrate = (audio["bitrate"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 },
        expiresAt = expiry,
    )
}

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun isHttpsStream(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
}.getOrDefault(false)
