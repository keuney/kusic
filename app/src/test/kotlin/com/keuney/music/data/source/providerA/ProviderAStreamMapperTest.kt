package com.keuney.music.data.source.providerA

import com.keuney.music.data.source.providerA.mapper.mapStreamResponse
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderAStreamMapperTest {
    private val now = Instant.parse("2026-09-02T00:00:00Z")

    @Test
    fun selectsDirectAudioAndMapsExpiryWithoutSelectingVideo() {
        val stream = mapStreamResponse(response(), now)
        assertEquals("https://example.invalid/audio-high", stream.url)
        assertEquals("audio/webm", stream.mimeType)
        assertEquals(128000, stream.bitrate)
        assertEquals(now.plusSeconds(3600), stream.expiresAt)
    }

    @Test
    fun missingExpiryRemainsUnknown() {
        assertNull(mapStreamResponse(response("null"), now).expiresAt)
    }

    @Test(expected = ProviderAStreamException::class)
    fun unsupportedTransportIsNotMistakenForAnAudioUrl() {
        mapStreamResponse(Json.parseToJsonElement("""
            {"playabilityStatus":{"status":"OK"},"streamingData":{
              "serverAbrStreamingUrl":"https://example.invalid/transport",
              "adaptiveFormats":[{"mimeType":"audio/webm","signatureCipher":"opaque-value"}]
            }}
        """).jsonObject, now)
    }

    @Test(expected = ProviderAStreamException::class)
    fun unavailableTrackDoesNotResolve() {
        mapStreamResponse(Json.parseToJsonElement("""{"playabilityStatus":{"status":"LOGIN_REQUIRED"}}""").jsonObject, now)
    }

    @Test(expected = ProviderAStreamException::class)
    fun nonHttpsAndCredentialBearingUrlsAreRejected() {
        mapStreamResponse(Json.parseToJsonElement("""
            {"playabilityStatus":{"status":"OK"},"streamingData":{"adaptiveFormats":[
              {"mimeType":"audio/mp4","url":"http://example.invalid/audio"},
              {"mimeType":"audio/mp4","url":"https://user:secret@example.invalid/audio"}
            ]}}
        """).jsonObject, now)
    }

    private fun response(expiry: String = "\"3600\"") = Json.parseToJsonElement("""
        {"playabilityStatus":{"status":"OK"},"streamingData":{
          "expiresInSeconds":$expiry,"adaptiveFormats":[
            {"mimeType":"video/mp4","url":"https://example.invalid/video","bitrate":9999999},
            {"mimeType":"audio/mp4","url":"https://example.invalid/audio-low","bitrate":64000},
            {"mimeType":"audio/webm; codecs=opus","url":"https://example.invalid/audio-high","bitrate":128000}
          ]
        }}
    """).jsonObject
}
