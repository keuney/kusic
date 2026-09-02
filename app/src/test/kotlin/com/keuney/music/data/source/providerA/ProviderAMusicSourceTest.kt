package com.keuney.music.data.source.providerA

import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAMusicSourceTest {
    @Test
    fun searchAndResolveStreamReachTheProviderImplementations(): Unit = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            val body = if (request.url.encodedPath.endsWith("search")) searchResponse else streamResponse
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = createMusicHttpClient(engine)
        try {
            val source = ProviderAMusicSource(
                ProviderASearch(ProviderAClient(http)),
                ProviderAStreamResolver(ProviderAClient(http)),
            )
            val tracks = source.search("아이유").getOrThrow()
            assertEquals(listOf("track-1"), tracks.map { it.id })
            assertEquals("video/mp4", source.resolveStream("track-1").getOrThrow().mimeType)
            assertEquals(listOf("/youtubei/v1/search", "/youtubei/v1/player"), paths)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun unimplementedOperationsFailSafelyWithoutCallingTheProvider(): Unit = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val http = createMusicHttpClient(engine)
        try {
            val source = ProviderAMusicSource(
                ProviderASearch(ProviderAClient(http)),
                ProviderAStreamResolver(ProviderAClient(http)),
            )
            assertTrue(source.getTrack("track-1").isFailure)
            assertTrue(source.getRelated("track-1").isFailure)
            assertEquals(
                "Source operation not available",
                source.getTrack("track-1").exceptionOrNull()?.message,
            )
            assertEquals(0, calls)
        } finally {
            http.close()
            engine.close()
        }
    }

    private val searchResponse = """
        {"contents":{"twoColumnSearchResultsRenderer":{"primaryContents":{"sectionListRenderer":{"contents":[
          {"itemSectionRenderer":{"contents":[
            {"videoRenderer":{"videoId":"track-1",
              "title":{"runs":[{"text":"곡 제목"}]},
              "ownerText":{"runs":[{"text":"아티스트"}]}}}
          ]}}
        ]}}}}}
    """.trimIndent()

    private val streamResponse = """
        {"playabilityStatus":{"status":"OK"},"streamingData":{"formats":[
          {"mimeType":"video/mp4","url":"https://example.invalid/progressive","bitrate":306098}
        ]}}
    """.trimIndent()
}
