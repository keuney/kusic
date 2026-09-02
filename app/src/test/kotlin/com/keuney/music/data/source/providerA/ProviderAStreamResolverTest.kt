package com.keuney.music.data.source.providerA

import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAStreamResolverTest {
    @Test
    fun sendsTrackAndPlayerVersionThenReturnsADomainStream(): Unit = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/youtubei/v1/player", request.url.encodedPath)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("test-track", body["videoId"]!!.jsonPrimitive.content)
            val context = body["playbackContext"]!!.jsonObject["contentPlaybackContext"]!!.jsonObject
            assertEquals(ProviderAConfig.signatureTimestamp.toString(), context["signatureTimestamp"]!!.jsonPrimitive.content)
            respond("""{"playabilityStatus":{"status":"OK"},"streamingData":{"adaptiveFormats":[
              {"mimeType":"audio/mp4","url":"https://example.invalid/audio"}
            ]}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = createMusicHttpClient(engine)
        try {
            assertEquals("audio/mp4", ProviderAStreamResolver(ProviderAClient(http)).resolveStream("test-track").getOrThrow().mimeType)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun emptyIdAndHttpErrorsAreSafeFailures(): Unit = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("sensitive response must not escape", HttpStatusCode.ServiceUnavailable)
        }
        val http = createMusicHttpClient(engine)
        try {
            val resolver = ProviderAStreamResolver(ProviderAClient(http))
            assertTrue(resolver.resolveStream(" ").isFailure)
            assertEquals(0, calls)
            assertEquals("Stream request failed", resolver.resolveStream("test-track").exceptionOrNull()?.message)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test(expected = CancellationException::class)
    fun cancellationPropagates(): Unit = runBlocking {
        val engine = MockEngine { throw CancellationException("cancelled") }
        val http = createMusicHttpClient(engine)
        try {
            ProviderAStreamResolver(ProviderAClient(http)).resolveStream("test-track")
        } finally {
            http.close()
            engine.close()
        }
    }
}
