package com.keuney.music.data.source.providerA

import com.keuney.music.data.network.createMusicHttpClient
import com.keuney.music.data.source.providerA.mapper.mapSearchResponse
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertTrue
import org.junit.Test

/** Live requests: excluded from regular unit tests by the Gradle task configuration. */
class ProviderASearchSourceContractTest {
    @Test fun koreanQuery(): Unit = verify("아이유")
    @Test fun popQuery(): Unit = verify("BTS Dynamite")
    @Test fun classicalQuery(): Unit = verify("Bach")

    private fun verify(query: String): Unit = runBlocking {
        val engine = OkHttp.create()
        val http = createMusicHttpClient(engine)
        try {
            val response = try {
                ProviderAClient(http).request("search", buildJsonObject {
                    put("query", query)
                })
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
            assertTrue(tracks.all { it.id.isNotBlank() && it.title.isNotBlank() })
        } finally { http.close(); engine.close() }
    }
}
