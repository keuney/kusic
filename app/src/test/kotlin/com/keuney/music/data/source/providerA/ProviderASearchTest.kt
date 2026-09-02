package com.keuney.music.data.source.providerA

import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderASearchTest {
    @Test
    fun blankQueryDoesNotMakeARequest(): Unit = runBlocking {
        val engine = MockEngine { error("Blank query must not access network") }
        val http = createMusicHttpClient(engine)
        try {
            assertEquals(emptyList<Any>(), ProviderASearch(ProviderAClient(http)).search("  ").getOrThrow())
        } finally { http.close(); engine.close() }
    }

    @Test
    fun emptySearchIsSuccessfulButHttpAndMalformedResponsesFail(): Unit = runBlocking {
        val responses = listOf(
            "{\"contents\":{\"sectionListRenderer\":{\"contents\":[]}}}" to HttpStatusCode.OK,
            "{}" to HttpStatusCode.ServiceUnavailable,
            "{broken" to HttpStatusCode.OK,
        ).iterator()
        val engine = MockEngine {
            val (body, status) = responses.next()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = createMusicHttpClient(engine)
        try {
            val search = ProviderASearch(ProviderAClient(http))
            assertEquals(emptyList<Any>(), search.search("test").getOrThrow())
            repeat(2) {
                val error = search.search("test").exceptionOrNull()
                assertTrue(error is ProviderASearchException)
                assertEquals("Source search unavailable", error?.message)
            }
        } finally { http.close(); engine.close() }
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotConvertedToAFailureValue(): Unit = runBlocking {
        val engine = MockEngine { throw CancellationException("cancelled") }
        val http = createMusicHttpClient(engine)
        try {
            ProviderASearch(ProviderAClient(http)).search("test")
        } finally { http.close(); engine.close() }
    }
}
