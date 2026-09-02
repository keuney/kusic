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

/**
 * progressive 형식만 재생 가능한 스트림으로 인정한다. 실기기 확인 결과 오디오 전용 adaptive
 * 주소는 어떤 구간 요청에도 403을 반환해 재생할 수 없다. 재생할 수 없는 전송 방식을 성공 값으로
 * 포장하지 않는다(ADR-031, ADR-034). progressive가 없으면 실패해 다음 클라이언트로 넘어간다.
 */
internal fun mapStreamResponse(response: JsonObject, now: Instant): PlayableStream {
    val status = (response["playabilityStatus"] as? JsonObject)?.string("status")
    if (status != "OK") throw ProviderAStreamException("Track is not playable")
    val data = response["streamingData"] as? JsonObject
        ?: throw ProviderAStreamException("Streaming metadata missing")
    val chosen = progressiveStream(data)
        ?: throw ProviderAStreamException("No direct audio stream available")
    val expirySeconds = (data["expiresInSeconds"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 }
    return PlayableStream(
        url = checkNotNull(chosen.string("url")),
        mimeType = chosen.string("mimeType")?.substringBefore(';'),
        bitrate = (chosen["bitrate"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 },
        expiresAt = expirySeconds?.let { runCatching { now.plusSeconds(it) }.getOrNull() },
    )
}

/** 오디오가 함께 들어 있는 단일 스트림. 컨테이너가 video/mp4여도 재생에는 오디오만 사용한다. */
private fun progressiveStream(data: JsonObject): JsonObject? =
    (data["formats"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        .filter { it.hasDirectStream() }
        .maxByOrNull { (it["bitrate"] as? JsonPrimitive)?.intOrNull ?: 0 }

private fun JsonObject.hasDirectStream(): Boolean = string("url")?.let(::isHttpsStream) == true

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun isHttpsStream(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
}.getOrDefault(false)
