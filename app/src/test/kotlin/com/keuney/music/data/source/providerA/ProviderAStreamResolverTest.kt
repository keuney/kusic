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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAStreamResolverTest {
    @Test
    fun signatureTimestampClientSendsPlayerVersionAndReturnsADomainStream(): Unit = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/youtubei/v1/player", request.url.encodedPath)
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("test-track", body["videoId"]!!.jsonPrimitive.content)
            assertEquals("WEB", body["context"]!!.jsonObject["client"]!!.jsonObject["clientName"]!!.jsonPrimitive.content)
            val context = body["playbackContext"]!!.jsonObject["contentPlaybackContext"]!!.jsonObject
            assertEquals(
                ProviderAConfig.signatureTimestamp.toString(),
                context["signatureTimestamp"]!!.jsonPrimitive.content,
            )
            respond(directAudioResponse, headers = jsonHeaders)
        }
        val http = createMusicHttpClient(engine)
        try {
            val stream = ProviderAStreamResolver(ProviderAClient(http))
                .resolveWith("test-track", ProviderAConfig.search).getOrThrow()
            assertEquals("video/mp4", stream.mimeType)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun directUrlClientOmitsPlayerVersionAndSendsItsOwnClientContext(): Unit = runBlocking {
        val profile = ProviderAConfig.streamCandidates.first { !it.sendsSignatureTimestamp }
        val engine = MockEngine { request ->
            assertEquals(profile.clientId, request.headers["X-Youtube-Client-Name"])
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[HttpHeaders.Cookie])
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertNull("직접 URL 종류에는 플레이어 버전을 보내지 않는다", body["playbackContext"])
            val client = body["context"]!!.jsonObject["client"]!!.jsonObject
            assertEquals(profile.clientName, client["clientName"]!!.jsonPrimitive.content)
            respond(directAudioResponse, headers = jsonHeaders)
        }
        val http = createMusicHttpClient(engine)
        try {
            assertTrue(ProviderAStreamResolver(ProviderAClient(http)).resolveWith("test-track", profile).isSuccess)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun fallsBackToTheNextClientWhenAResponseHasNoDirectUrl(): Unit = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            if (calls == 1) respond(sabrOnlyResponse, headers = jsonHeaders)
            else respond(directAudioResponse, headers = jsonHeaders)
        }
        val http = createMusicHttpClient(engine)
        try {
            val stream = ProviderAStreamResolver(ProviderAClient(http)).resolveStream("test-track").getOrThrow()
            assertEquals("video/mp4", stream.mimeType)
            assertEquals(2, calls)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun everyClientFailingKeepsTheLastSafeFailure(): Unit = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(sabrOnlyResponse, headers = jsonHeaders)
        }
        val http = createMusicHttpClient(engine)
        try {
            val result = ProviderAStreamResolver(ProviderAClient(http)).resolveStream("test-track")
            assertEquals("No direct audio stream available", result.exceptionOrNull()?.message)
            assertEquals(ProviderAConfig.streamCandidates.size, calls)
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

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val directAudioResponse = """
        {"playabilityStatus":{"status":"OK"},"streamingData":{"formats":[
          {"mimeType":"video/mp4","url":"https://example.invalid/progressive","bitrate":306098}
        ]}}
    """.trimIndent()

    private val sabrOnlyResponse = """
        {"playabilityStatus":{"status":"OK"},"streamingData":{
          "serverAbrStreamingUrl":"https://example.invalid/sabr",
          "adaptiveFormats":[{"mimeType":"audio/mp4","bitrate":128000}]
        }}
    """.trimIndent()
}
