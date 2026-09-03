package com.keuney.music.data.network

import com.keuney.music.core.model.AppError
import com.keuney.music.data.source.toAppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** KM-062: connect/read/request 대기 상한과 취소 존중을 확인한다. */
class NetworkTimeoutPolicyTest {
    @Test
    fun theClientUsesTheDeclaredPolicyByDefault() {
        val defaults = SourceTimeouts()

        assertEquals(NetworkTimeouts.CONNECT_MS.toLong(), defaults.connectMs)
        assertEquals(NetworkTimeouts.SOURCE_SOCKET_MS.toLong(), defaults.socketMs)
        assertEquals(NetworkTimeouts.SOURCE_REQUEST_MS.toLong(), defaults.requestMs)
    }

    @Test
    fun timeoutValuesFormAConsistentPolicy() {
        assertTrue(
            "연결 대기는 바이트 대기보다 짧아야 한다",
            NetworkTimeouts.CONNECT_MS < NetworkTimeouts.SOURCE_SOCKET_MS,
        )
        assertTrue(
            "요청 전체 상한은 바이트 대기 이상이어야 한다",
            NetworkTimeouts.SOURCE_REQUEST_MS >= NetworkTimeouts.SOURCE_SOCKET_MS,
        )
        assertTrue(
            "재생은 media3 기본 8초보다 오래 기다린다",
            NetworkTimeouts.PLAYBACK_READ_MS > 8_000,
        )
    }

    @Test
    fun aRequestThatExceedsTheLimitFailsAsANetworkError(): Unit = runBlocking {
        val engine = MockEngine {
            delay(5_000)
            respond("{}", headers = jsonHeaders)
        }
        val http = createMusicHttpClient(engine, shortTimeouts)
        try {
            http.get("https://example.invalid/slow")
            fail("상한을 넘긴 요청은 실패해야 한다")
        } catch (failure: Exception) {
            assertEquals("상한 초과는 네트워크 오류로 보여야 한다", AppError.Network, failure.toAppError())
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun aResponseWithinTheLimitSucceeds(): Unit = runBlocking {
        val engine = MockEngine {
            delay(50)
            respond("{}", headers = jsonHeaders)
        }
        val http = createMusicHttpClient(engine, shortTimeouts)
        try {
            assertEquals(200, http.get("https://example.invalid/fast").status.value)
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun cancellingTheCallerStopsTheRequestWithoutWaitingForTheLimit(): Unit = runBlocking {
        var started = false
        val engine = MockEngine {
            started = true
            delay(60_000)
            respond("{}", headers = jsonHeaders)
        }
        val http = createMusicHttpClient(engine)
        try {
            val call = async { http.get("https://example.invalid/slow") }
            while (!started) yield()
            call.cancel()

            try {
                call.await()
                fail("취소된 요청이 값을 돌려주면 안 된다")
            } catch (_: CancellationException) {
                // 기대한 동작. 30초 상한을 기다리지 않고 즉시 끝난다.
            }
        } finally {
            http.close()
            engine.close()
        }
    }

    private val shortTimeouts = SourceTimeouts(connectMs = 300, socketMs = 300, requestMs = 500)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
}
