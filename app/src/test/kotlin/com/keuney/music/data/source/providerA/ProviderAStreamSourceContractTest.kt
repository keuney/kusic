package com.keuney.music.data.source.providerA

import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.prepareGet
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAStreamSourceContractTest {
    @Test
    fun resolvesARealTrackAndAcceptsARangeRequest(): Unit = runBlocking {
        val engine = OkHttp.create()
        val http = createMusicHttpClient(engine)
        try {
            val result = ProviderAStreamResolver(ProviderAClient(http)).resolveStream("gdZLi9oWNZg")
            assertTrue("직접 재생 가능한 오디오 스트림 해석 실패", result.isSuccess)
            val stream = result.getOrThrow()
            val readable = try {
                http.prepareGet(stream.url) { header(HttpHeaders.Range, "bytes=0-31") }.execute {
                    it.status == HttpStatusCode.PartialContent
                }
            } catch (_: Exception) {
                false
            }
            assertTrue("해석된 스트림의 부분 요청 실패", readable)
        } finally {
            http.close()
            engine.close()
        }
    }
}
