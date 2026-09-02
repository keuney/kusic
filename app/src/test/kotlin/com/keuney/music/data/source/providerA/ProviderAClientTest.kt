package com.keuney.music.data.source.providerA

import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class ProviderAClientTest {
    @Test
    fun sendsCentralContextAndHeadersWithoutCredentials(): Unit = runBlocking {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/youtubei/v1/search", request.url.encodedPath)
            assertEquals(ProviderAConfig.clientId, request.headers["X-Youtube-Client-Name"])
            assertEquals(ProviderAConfig.clientVersion, request.headers["X-Youtube-Client-Version"])
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[HttpHeaders.Cookie])
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            assertEquals("test query", body["query"]!!.jsonPrimitive.content)
            val client = body["context"]!!.jsonObject["client"]!!.jsonObject
            assertEquals("WEB", client["clientName"]!!.jsonPrimitive.content)
            assertEquals(ProviderAConfig.clientVersion, client["clientVersion"]!!.jsonPrimitive.content)
            respond("{\"contents\":{}}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = createMusicHttpClient(engine)
        try {
            val response = ProviderAClient(http).request("search", buildJsonObject {
                put("query", "test query")
                put("context", "must not override central configuration")
            })
            assertEquals(buildJsonObject {}, response["contents"])
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun httpFailureIsNotMistakenForASuccessfulPayload(): Unit = runBlocking {
        val engine = MockEngine { respond("{}", HttpStatusCode.ServiceUnavailable) }
        val http = createMusicHttpClient(engine)
        try {
            try {
                ProviderAClient(http).request("search", buildJsonObject {})
                fail("HTTP 오류는 성공 값으로 반환하면 안 됨")
            } catch (expected: ServerResponseException) {
                assertEquals(HttpStatusCode.ServiceUnavailable, expected.response.status)
            }
        } finally {
            http.close()
            engine.close()
        }
    }
}
