package com.keuney.music.data.source.providerA

import com.keuney.music.data.source.providerA.mapper.mapSearchResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderASearchMapperTest {
    @Test
    fun mapsMusicMetadataAndDeduplicatesTrackIds() {
        val track = mapSearchResponse(response(item(), item())).single()
        assertEquals("test-track", track.id)
        assertEquals("Test song", track.title)
        assertEquals("Test artist", track.artist)
        assertEquals(3_723_000L, track.durationMs)
        assertEquals("https://example.invalid/art.jpg", track.artworkUrl)
    }

    @Test
    fun ignoresNonVideoResultsAndAllowsMissingOptionalMetadata() {
        assertEquals(emptyList<Any>(), mapSearchResponse(response("{\"channelRenderer\":{\"title\":\"Channel\"}}")))
        val track = mapSearchResponse(response(item().replace("1:02:03", "LIVE")
            .replace("https://example.invalid/art.jpg", "http://example.invalid/art.jpg"))).single()
        assertNull(track.durationMs)
        assertNull(track.artworkUrl)
    }

    @Test
    fun emptySectionsAreAnEmptySearch() {
        assertEquals(emptyList<Any>(), mapSearchResponse(response()))
    }

    @Test(expected = IllegalStateException::class)
    fun unexpectedResponseIsNotAnEmptySearch() {
        mapSearchResponse(Json.parseToJsonElement("{\"changed\":true}").jsonObject)
    }

    @Test(expected = IllegalStateException::class)
    fun musicWithoutAnIdIsAContractFailure() {
        mapSearchResponse(response(item().replace("test-track", "")))
    }

    private fun response(vararg items: String) = Json.parseToJsonElement(
        """{"contents":{"sectionListRenderer":{"contents":[${items.joinToString(",")}]}}}""",
    ).jsonObject

    private fun item() = """
        {"videoRenderer":{
          "videoId":"test-track",
          "title":{"runs":[{"text":"Test song"}]},
          "ownerText":{"runs":[{"text":"Test artist"}]},
          "lengthText":{"simpleText":"1:02:03"},
          "thumbnail":{"thumbnails":[{"url":"https://example.invalid/art.jpg"}]}
        }}
    """.trimIndent()
}
