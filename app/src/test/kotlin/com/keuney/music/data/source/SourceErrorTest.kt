package com.keuney.music.data.source

import com.keuney.music.core.model.AppError
import com.keuney.music.data.network.createMusicHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** KM-060: 공급자·인프라 오류를 사용자에게 보여줄 다섯 분류로 바꾼다. */
class SourceErrorTest {
    @Test
    fun everyFailureCategoryHasAUserFacingError() {
        assertEquals(AppError.Network, SourceFailure.Network.toAppError())
        assertEquals(AppError.SourceUnavailable, SourceFailure.Parse.toAppError())
        assertEquals(AppError.PlaybackUnavailable, SourceFailure.NotFound.toAppError())
        assertEquals(AppError.PlaybackUnavailable, SourceFailure.Restricted.toAppError())
        assertEquals(AppError.Unknown, SourceFailure.Unknown.toAppError())
    }

    @Test
    fun everyFailureCategoryIsMapped() {
        // 새 분류를 추가하면 매핑을 빠뜨리지 않도록 전수 확인한다.
        SourceFailure.entries.forEach { it.toAppError() }
        assertEquals(5, SourceFailure.entries.size)
    }

    @Test
    fun httpStatusesAreClassified() {
        assertEquals(SourceFailure.Restricted, httpStatusToFailure(401))
        assertEquals(SourceFailure.Restricted, httpStatusToFailure(403))
        assertEquals(SourceFailure.NotFound, httpStatusToFailure(404))
        assertEquals(SourceFailure.NotFound, httpStatusToFailure(410))
        assertEquals(SourceFailure.Network, httpStatusToFailure(408))
        assertEquals(SourceFailure.Network, httpStatusToFailure(429))
        assertEquals(SourceFailure.Network, httpStatusToFailure(503))
        assertEquals(SourceFailure.Unknown, httpStatusToFailure(418))
    }

    @Test
    fun sourceFailuresKeepTheirOwnClassification() {
        val restricted = object : Exception("고정 문자열"), SourceFailureAware {
            override val failure = SourceFailure.Restricted
        }
        assertEquals(AppError.PlaybackUnavailable, restricted.toAppError())
    }

    @Test
    fun infrastructureExceptionsBecomeNetworkOrSourceErrors() {
        assertEquals(AppError.Network, UnknownHostException("host").toAppError())
        assertEquals(AppError.Network, IOException("socket").toAppError())
        assertEquals(AppError.SourceUnavailable, SerializationException("bad json").toAppError())
        assertEquals(AppError.Unknown, IllegalArgumentException("etc").toAppError())
    }

    @Test
    fun realHttpFailuresAreClassifiedFromTheirStatus(): Unit = runBlocking {
        assertEquals(AppError.PlaybackUnavailable, appErrorForStatus(HttpStatusCode.NotFound))
        assertEquals(AppError.PlaybackUnavailable, appErrorForStatus(HttpStatusCode.Forbidden))
        assertEquals(AppError.Network, appErrorForStatus(HttpStatusCode.ServiceUnavailable))
    }

    @Test
    fun userFacingErrorsNeverCarryTheOriginalMessage() {
        val secret = "sensitive-test-value"
        val mapped = IllegalStateException(secret).toAppError()

        assertFalse(mapped.toString().contains(secret))
        assertEquals(AppError.Unknown, mapped)
    }

    private suspend fun appErrorForStatus(status: HttpStatusCode): AppError {
        val engine = MockEngine { respond("응답 원문은 노출되지 않는다", status) }
        val http = createMusicHttpClient(engine)
        return try {
            http.get("https://example.invalid/resource")
            error("실패해야 하는 요청이 성공했다")
        } catch (failure: ResponseException) {
            failure.toAppError()
        } finally {
            http.close()
            engine.close()
        }
    }
}
