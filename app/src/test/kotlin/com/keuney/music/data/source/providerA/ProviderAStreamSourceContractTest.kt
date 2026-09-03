package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.PlayableStream
import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 공급자의 재생 전송 방식 변경을 조기에 감지한다. 후보 클라이언트를 모두 시도해
 * 어떤 종류가 재생 가능한 스트림을 주는지 기록한다. URL과 응답 원문은 출력하지 않는다.
 *
 * 지키는 것: 파일 앞부분뿐 아니라 중간 지점 구간 요청까지 받아들이는지.
 * 앞부분만 되는 주소는 이어 재생이 불가능하며 실제로 그런 시기가 있었다(ADR-034).
 */
class ProviderAStreamSourceContractTest {
    @Test
    fun aRealTrackResolvesToAStreamThatServesItsMiddleAsWell(): Unit = runBlocking {
        val engine = OkHttp.create()
        val http = createMusicHttpClient(engine)
        try {
            val resolver = ProviderAStreamResolver(ProviderAClient(http))
            val report = StringBuilder("재생 스트림 해석 결과 (트랙 $TRACK_ID, URL 비출력)\n")
            var playable: String? = null
            for (profile in ProviderAConfig.streamCandidates) {
                val result = resolver.resolveWith(TRACK_ID, profile)
                val stream = result.getOrNull()
                val probe = if (stream == null) Probe() else probeMiddle(http, stream)
                report.append(describe(profile.clientName, result.exceptionOrNull()?.message, stream, probe))
                if (playable == null && probe.servesMiddle) playable = profile.clientName
            }
            println(report)
            assertNotNull("중간 지점까지 이어 받을 수 있는 클라이언트가 없음\n$report", playable)
        } finally {
            http.close()
            engine.close()
        }
    }

    private data class Probe(
        val totalBytes: Long = 0,
        val middleStatus: Int = -1,
    ) {
        val servesMiddle: Boolean get() = middleStatus == PARTIAL_CONTENT
    }

    /** 앞부분 응답의 Content-Range로 전체 길이를 얻고 절반 지점을 다시 요청한다. */
    private suspend fun probeMiddle(http: HttpClient, stream: PlayableStream): Probe {
        val total = runCatching {
            http.rangeRequest(stream, "bytes=0-31") { response ->
                response.headers[HttpHeaders.ContentRange]?.substringAfter('/', "")?.toLongOrNull() ?: 0L
            }
        }.getOrDefault(0L)
        if (total <= 0) return Probe()
        val from = total / 2
        val status = try {
            http.rangeRequest(stream, "bytes=$from-${from + 31}") { it.status.value }
        } catch (rejected: ResponseException) {
            rejected.response.status.value
        } catch (_: Exception) {
            -1
        }
        return Probe(total, status)
    }

    /** 재생 요청은 앱과 동일하게 해석 시 사용한 클라이언트 헤더를 함께 보낸다. */
    private suspend fun <T> HttpClient.rangeRequest(
        stream: PlayableStream,
        range: String,
        read: suspend (io.ktor.client.statement.HttpResponse) -> T,
    ): T = prepareGet(stream.url) {
        stream.requestHeaders.forEach { (name, value) -> header(name, value) }
        header(HttpHeaders.Range, range)
    }.execute { read(it) }

    private fun describe(
        clientName: String,
        failure: String?,
        stream: PlayableStream?,
        probe: Probe,
    ): String = buildString {
        append("- ").append(clientName).append(": ")
        if (stream == null) {
            append("해석 실패 — ").append(failure ?: "원인 미상")
        } else {
            append("해석 성공, ").append(stream.mimeType ?: "mime 미상")
            append(", bitrate=").append(stream.bitrate ?: 0)
            append(", 크기=").append(probe.totalBytes / 1024).append("KB")
            append(", 만료시각=").append(if (stream.expiresAt == null) "없음" else "있음")
            append(", 중간구간=").append(probe.middleStatus)
            append(' ').append(if (probe.servesMiddle) "PASS" else "FAIL")
        }
        append('\n')
    }

    private companion object {
        const val TRACK_ID = "gdZLi9oWNZg"
        const val PARTIAL_CONTENT = 206
    }
}
