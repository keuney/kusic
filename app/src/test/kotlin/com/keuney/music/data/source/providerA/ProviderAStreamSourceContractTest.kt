package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.PlayableStream
import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 공급자의 재생 전송 방식 변경을 조기에 감지한다. 후보 클라이언트를 모두 시도해
 * 어떤 종류가 직접 오디오 URL을 제공하는지 기록한다. URL과 응답 원문은 출력하지 않는다.
 */
class ProviderAStreamSourceContractTest {
    @Test
    fun aRealTrackResolvesToAPlayableAudioStreamOnAtLeastOneClient(): Unit = runBlocking {
        val engine = OkHttp.create()
        val http = createMusicHttpClient(engine)
        try {
            val resolver = ProviderAStreamResolver(ProviderAClient(http))
            val report = StringBuilder("재생 스트림 해석 결과 (트랙 $TRACK_ID, URL 비출력)\n")
            var playable: String? = null
            for (profile in ProviderAConfig.streamCandidates) {
                val result = resolver.resolveWith(TRACK_ID, profile)
                val stream = result.getOrNull()
                val readable = stream != null && acceptsRangeRequest(http, stream)
                report.append(describe(profile.clientName, result.exceptionOrNull()?.message, stream, readable))
                if (playable == null && readable) playable = profile.clientName
            }
            println(report)
            assertNotNull("직접 재생 가능한 오디오 스트림을 제공하는 클라이언트 없음\n$report", playable)
        } finally {
            http.close()
            engine.close()
        }
    }

    private fun describe(
        clientName: String,
        failure: String?,
        stream: PlayableStream?,
        readable: Boolean,
    ): String = buildString {
        append("- ").append(clientName).append(": ")
        if (stream == null) {
            append("해석 실패 — ").append(failure ?: "원인 미상")
        } else {
            append("해석 성공, ").append(stream.mimeType ?: "mime 미상")
            append(", bitrate=").append(stream.bitrate ?: 0)
            append(", 만료시각=").append(if (stream.expiresAt == null) "없음" else "있음")
            append(", 부분요청=").append(if (readable) "PASS" else "FAIL")
        }
        append('\n')
    }

    /** 재생 요청은 앱과 동일하게 해석 시 사용한 클라이언트 헤더를 함께 보낸다. */
    private suspend fun acceptsRangeRequest(http: HttpClient, stream: PlayableStream): Boolean = try {
        http.prepareGet(stream.url) {
            stream.requestHeaders.forEach { (name, value) -> header(name, value) }
            header(HttpHeaders.Range, "bytes=0-31")
        }.execute {
            it.status == HttpStatusCode.PartialContent
        }
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val TRACK_ID = "gdZLi9oWNZg"
    }
}
