package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.AppError
import com.keuney.music.data.network.createMusicHttpClient
import com.keuney.music.data.source.SourceFailure
import com.keuney.music.data.source.SourceFailureAware
import com.keuney.music.data.source.toAppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** KM-060: 공급자 응답과 실패가 도메인 오류로 이어지는지 확인한다. */
class ProviderAErrorMappingTest {
    @Test
    fun playabilityStatusesAreClassified(): Unit = runBlocking {
        assertEquals(SourceFailure.Restricted, streamFailureFor("LOGIN_REQUIRED"))
        assertEquals(SourceFailure.Restricted, streamFailureFor("AGE_CHECK_REQUIRED"))
        assertEquals(SourceFailure.Restricted, streamFailureFor("CONTENT_CHECK_REQUIRED"))
        assertEquals(SourceFailure.NotFound, streamFailureFor("UNPLAYABLE"))
        assertEquals(SourceFailure.NotFound, streamFailureFor("ERROR"))
        assertEquals(SourceFailure.Unknown, streamFailureFor("SOMETHING_NEW"))
    }

    @Test
    fun aMissingStatusOrTransportIsASourceProblem(): Unit = runBlocking {
        // 상태 필드 자체가 없으면 응답 구조가 바뀐 것이다.
        assertEquals(AppError.SourceUnavailable, streamErrorFor("""{"streamingData":{}}"""))
        // 상태는 정상인데 재생 가능한 전송 방식이 없는 경우도 소스 문제로 본다.
        assertEquals(
            AppError.SourceUnavailable,
            streamErrorFor(
                """{"playabilityStatus":{"status":"OK"},"streamingData":{
                  "serverAbrStreamingUrl":"https://example.invalid/sabr"
                }}""",
            ),
        )
    }

    @Test
    fun httpFailuresDuringStreamResolutionBecomeUserFacingErrors(): Unit = runBlocking {
        assertEquals(AppError.PlaybackUnavailable, streamErrorForStatus(HttpStatusCode.NotFound))
        assertEquals(AppError.PlaybackUnavailable, streamErrorForStatus(HttpStatusCode.Forbidden))
        assertEquals(AppError.Network, streamErrorForStatus(HttpStatusCode.ServiceUnavailable))
    }

    @Test
    fun searchFailuresAreClassifiedByCause(): Unit = runBlocking {
        // 응답 구조가 예상과 다르면 소스 문제다.
        assertEquals(AppError.SourceUnavailable, searchErrorFor("""{"contents":{}}""", HttpStatusCode.OK))
        // HTTP 오류는 상태에 따라 분류한다.
        assertEquals(AppError.Network, searchErrorFor("{}", HttpStatusCode.ServiceUnavailable))
        assertEquals(AppError.PlaybackUnavailable, searchErrorFor("{}", HttpStatusCode.NotFound))
    }

    @Test
    fun unimplementedOperationsAreNotReportedAsUnknown(): Unit = runBlocking {
        val source = ProviderAMusicSource(
            ProviderASearch(ProviderAClient(clientFor("{}", HttpStatusCode.OK))),
            ProviderAStreamResolver(ProviderAClient(clientFor("{}", HttpStatusCode.OK))),
        )
        val failure = source.getTrack("track-1").exceptionOrNull()

        assertEquals(SourceFailure.NotFound, (failure as SourceFailureAware).failure)
        assertEquals(AppError.PlaybackUnavailable, failure.toAppError())
    }

    private fun clientFor(body: String, status: HttpStatusCode) = createMusicHttpClient(
        MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
    )

    private suspend fun streamFailureFor(status: String): SourceFailure {
        val error = resolveFailure("""{"playabilityStatus":{"status":"$status"}}""", HttpStatusCode.OK)
        return (error as SourceFailureAware).failure
    }

    private suspend fun streamErrorFor(body: String): AppError =
        resolveFailure(body, HttpStatusCode.OK).toAppError()

    private suspend fun streamErrorForStatus(status: HttpStatusCode): AppError =
        resolveFailure("{}", status).toAppError()

    private suspend fun resolveFailure(body: String, status: HttpStatusCode): Throwable {
        val http = clientFor(body, status)
        return try {
            val resolver = ProviderAStreamResolver(ProviderAClient(http))
            requireNotNull(
                resolver.resolveWith("track-1", ProviderAConfig.streamCandidates.first()).exceptionOrNull(),
            )
        } finally {
            http.close()
        }
    }

    private suspend fun searchErrorFor(body: String, status: HttpStatusCode): AppError {
        val http = clientFor(body, status)
        return try {
            val failure = ProviderASearch(ProviderAClient(http)).search("아이유").exceptionOrNull()
            requireNotNull(failure).toAppError()
        } finally {
            http.close()
        }
    }
}
