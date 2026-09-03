package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.Track
import com.keuney.music.data.network.createMusicHttpClient
import com.keuney.music.data.source.providerA.mapper.mapSearchResponse
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 실제 검색 요청. 일반 단위 검사에서는 Gradle 설정으로 제외한다.
 *
 * 지키는 것: 검색 경로가 살아 있는지, 응답 구조가 mapper의 기대와 맞는지,
 * 화면이 쓰는 메타데이터(제목·아티스트·길이·이미지)가 계속 채워지는지.
 * 구조가 바뀌면 mapper가 실패하거나 메타데이터 비율이 떨어져 여기서 먼저 드러난다.
 */
class ProviderASearchSourceContractTest {
    @Test fun koreanQuery(): Unit = verify("아이유")
    @Test fun popQuery(): Unit = verify("BTS Dynamite")
    @Test fun classicalQuery(): Unit = verify("Bach")

    private fun verify(query: String): Unit = runBlocking {
        val engine = OkHttp.create()
        val http = createMusicHttpClient(engine)
        try {
            val response = try {
                ProviderAClient(http).request("search", buildJsonObject { put("query", query) })
            } catch (failure: Exception) {
                val kinds = generateSequence<Throwable>(failure) { it.cause }
                    .take(5).joinToString(" -> ") { it.javaClass.simpleName }
                throw AssertionError("검색 요청 실패: $query ($kinds)")
            }
            val tracks = try {
                mapSearchResponse(response)
            } catch (_: Exception) {
                throw AssertionError("검색 응답 구조 불일치: $query")
            }

            assertTrue("실제 곡 검색 결과가 없음: $query", tracks.isNotEmpty())
            assertTrue(
                "ID나 제목이 빈 결과가 있음: $query",
                tracks.all { it.id.isNotBlank() && it.title.isNotBlank() },
            )
            assertTrue(
                "중복 ID가 섞여 있음: $query",
                tracks.map(Track::id).distinct().size == tracks.size,
            )
            // 선택 메타데이터는 곡마다 없을 수 있으나, 대부분이 비면 응답 구조가 바뀐 것이다.
            assertMostlyPresent(query, "아티스트", tracks) { it.artist.isNotBlank() }
            assertMostlyPresent(query, "길이", tracks) { (it.durationMs ?: 0) > 0 }
            assertMostlyPresent(query, "이미지", tracks) { it.artworkUrl?.startsWith("https://") == true }
        } finally {
            http.close()
            engine.close()
        }
    }

    private fun assertMostlyPresent(
        query: String,
        field: String,
        tracks: List<Track>,
        present: (Track) -> Boolean,
    ) {
        val count = tracks.count(present)
        assertTrue(
            "$field 가 채워진 결과가 너무 적다: $query ($count/${tracks.size})",
            count * 2 >= tracks.size,
        )
    }
}
