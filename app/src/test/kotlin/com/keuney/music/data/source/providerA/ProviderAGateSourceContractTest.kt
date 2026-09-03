package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.Track
import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KM-059 Provider A Gate의 소스 판정. 여러 아티스트의 실제 10곡과 긴 곡 하나를 대상으로
 * 검색 → 스트림 해석 → 파일 중간 지점 구간 요청까지 확인한다. URL은 출력하지 않는다.
 */
class ProviderAGateSourceContractTest {
    @Test
    fun tenRealTracksFromMultipleArtistsResolveToPlayableStreams(): Unit = runBlocking {
        val engine = OkHttp.create()
        val http = createMusicHttpClient(engine)
        val client = ProviderAClient(http)
        try {
            val tracks = collectTracks(client)
            val report = StringBuilder("Provider A Gate 판정 (URL 비출력)\n")
            report.append("검색어 ${QUERIES.size}종에서 모은 트랙 ${tracks.size}곡\n")
            assertTrue("검색으로 10곡을 모으지 못함: ${tracks.size}", tracks.size >= REQUIRED_TRACKS)

            val resolver = ProviderAStreamResolver(client)
            var resolved = 0
            var readable = 0
            for (track in tracks) {
                delay(1_000)
                val stream = resolver.resolveStream(track.id).getOrNull()
                if (stream != null) resolved++
                val probe = if (stream == null) {
                    FarRange(-1, 0)
                } else {
                    farRange(http, stream.url, stream.requestHeaders)
                }
                if (probe.status == PARTIAL_CONTENT) readable++
                report.append(
                    "- ${track.title.take(26)} | ${track.artist.take(14)} | " +
                        "${(track.durationMs ?: 0) / 1000}초 | " +
                        "해석 ${if (stream != null) "PASS" else "FAIL"} | " +
                        "크기 ${probe.totalBytes / 1024}KB | " +
                        "중간구간 ${probe.status} ${if (probe.status == PARTIAL_CONTENT) "PASS" else "FAIL"}\n",
                )
            }
            val longestSeconds = (tracks.maxOfOrNull { it.durationMs ?: 0 } ?: 0) / 1000
            val artists = tracks.map(Track::artist).distinct().size
            report.append("해석 $resolved/${tracks.size}, 중간 구간 $readable/${tracks.size}, ")
            report.append("아티스트 ${artists}종, 가장 긴 곡 ${longestSeconds}초\n")
            println(report)

            assertTrue("긴 곡(7분 이상)이 세트에 없음\n$report", longestSeconds >= LONG_TRACK_SECONDS)
            assertTrue("서로 다른 아티스트가 3명 미만\n$report", artists >= 3)
            assertTrue("스트림 해석 실패가 있음: $resolved/${tracks.size}\n$report", resolved == tracks.size)
            assertTrue("중간 구간 요청 실패가 있음: $readable/${tracks.size}\n$report", readable == tracks.size)
        } finally {
            http.close()
            engine.close()
        }
    }

    private suspend fun collectTracks(client: ProviderAClient): List<Track> {
        val search = ProviderASearch(client)
        val collected = LinkedHashMap<String, Track>()
        for (query in QUERIES) {
            delay(1_000)
            // 검색어마다 상위 몇 곡만 취해 아티스트가 한쪽으로 쏠리지 않게 한다.
            search.search(query).getOrElse { emptyList() }
                .take(3)
                .forEach { collected.putIfAbsent(it.id, it) }
            if (collected.size >= REQUIRED_TRACKS) break
        }
        return collected.values.take(REQUIRED_TRACKS).toList()
    }

    private data class FarRange(val status: Int, val totalBytes: Long)

    /**
     * 파일 앞부분만 받아도 통과하지 않도록 전체 길이의 절반 지점을 요청한다.
     * 전체 길이는 앞부분 응답의 Content-Range에서 얻어, 파일 끝을 넘는 요청과 실제 거부를 구분한다.
     */
    private suspend fun farRange(
        http: HttpClient,
        url: String,
        headers: Map<String, String>,
    ): FarRange {
        val total = runCatching {
            http.prepareGet(url) {
                headers.forEach { (name, value) -> header(name, value) }
                header(HttpHeaders.Range, "bytes=0-31")
            }.execute { response ->
                response.headers[HttpHeaders.ContentRange]?.substringAfter('/', "")?.toLongOrNull() ?: 0L
            }
        }.getOrDefault(0L)
        if (total <= 0) return FarRange(-1, 0)
        val from = total / 2
        val status = try {
            http.prepareGet(url) {
                headers.forEach { (name, value) -> header(name, value) }
                header(HttpHeaders.Range, "bytes=$from-${from + 31}")
            }.execute { it.status.value }
        } catch (rejected: ResponseException) {
            rejected.response.status.value
        } catch (failure: Exception) {
            println("  중간 구간 요청 예외: ${failure.javaClass.simpleName}")
            -1
        }
        return FarRange(status, total)
    }

    private companion object {
        const val REQUIRED_TRACKS = 10
        const val LONG_TRACK_SECONDS = 7 * 60L
        const val PARTIAL_CONTENT = 206
        val QUERIES = listOf(
            "아이유 좋은 날",
            "BTS Dynamite",
            "Beethoven Symphony No 9 full",
            "Queen Bohemian Rhapsody",
            "Jazz piano long mix",
        )
    }
}
