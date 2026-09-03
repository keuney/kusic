package com.keuney.music.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** 메타데이터 요청의 대기 상한. 기본값은 [NetworkTimeouts]이며 검사에서만 짧게 바꾼다. */
internal data class SourceTimeouts(
    val connectMs: Long = NetworkTimeouts.CONNECT_MS.toLong(),
    val socketMs: Long = NetworkTimeouts.SOURCE_SOCKET_MS.toLong(),
    val requestMs: Long = NetworkTimeouts.SOURCE_REQUEST_MS.toLong(),
)

/** 검색·스트림 주소 해석 같은 메타데이터 요청에 쓰는 클라이언트. */
internal fun createMusicHttpClient(
    engine: HttpClientEngine,
    timeouts: SourceTimeouts = SourceTimeouts(),
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(HttpTimeout) {
        connectTimeoutMillis = timeouts.connectMs
        socketTimeoutMillis = timeouts.socketMs
        requestTimeoutMillis = timeouts.requestMs
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
