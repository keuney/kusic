package com.keuney.music.data.network

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MusicHttpClientTest {
    @Test
    fun appliesTimeoutsAndDecodesJsonWithUnknownFields() = runBlocking {
        val engine = MockEngine { request ->
            val timeouts = request.getCapabilityOrNull(HttpTimeoutCapability)!!
            assertEquals(10_000L, timeouts.connectTimeoutMillis)
            assertEquals(20_000L, timeouts.socketTimeoutMillis)
            assertEquals(30_000L, timeouts.requestTimeoutMillis)
            respond("""{"value":"ok","extra":true}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createMusicHttpClient(engine)
        try {
            assertEquals("ok", client.get("https://test.invalid/sample").body<SampleResponse>().value)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun requestTimeoutTerminatesAStalledRequest() = runBlocking {
        val engine = MockEngine { awaitCancellation() }
        val client = createMusicHttpClient(engine)
        try {
            try {
                client.get("https://test.invalid/stalled") { timeout { requestTimeoutMillis = 50 } }
                fail("요청이 제한 시간 내 종료되어야 함")
            } catch (_: HttpRequestTimeoutException) {
                // 예상한 제한 시간 만료.
            }
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun callerCancellationStopsTheRequest() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val engine = MockEngine {
            entered.complete(Unit)
            awaitCancellation()
        }
        val client = createMusicHttpClient(engine)
        try {
            val request = async { client.get("https://test.invalid/cancel") }
            entered.await()
            request.cancelAndJoin()
            assertTrue(request.isCancelled)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Serializable
    private data class SampleResponse(val value: String)
}
