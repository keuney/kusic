package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.AppError
import com.keuney.music.data.network.createMusicHttpClient
import com.keuney.music.data.source.SourceFailure
import com.keuney.music.data.source.SourceFailureAware
import com.keuney.music.data.source.toAppError
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 실제 실패 응답에 대한 계약. KM-060의 오류 분류가 공급자의 현재 동작과 맞는지 확인한다.
 * 단위 검사는 우리가 만든 응답으로만 검증하므로, 공급자가 실패를 알리는 방식을 바꾸면
 * 단위 검사는 통과하면서 사용자에게 엉뚱한 문구가 보일 수 있다. 그 간극을 여기서 막는다.
 */
class ProviderAErrorSourceContractTest {
    @Test
    fun anUnknownTrackFailsWithAUserFacingPlaybackError(): Unit = runBlocking {
        val engine = OkHttp.create()
        val http = createMusicHttpClient(engine)
        try {
            val result = ProviderAStreamResolver(ProviderAClient(http)).resolveStream(UNKNOWN_TRACK_ID)
            val failure = result.exceptionOrNull()

            assertTrue("없는 트랙이 해석에 성공했다", result.isFailure)
            val classified = (failure as? SourceFailureAware)?.failure
            println("없는 트랙 분류: $classified, 사용자 오류: ${failure?.toAppError()}")

            assertTrue(
                "없는 트랙은 재생 불가 계열로 분류해야 한다: $classified",
                classified == SourceFailure.NotFound || classified == SourceFailure.Restricted,
            )
            assertEquals(
                "사용자에게는 재생 불가로 보여야 한다",
                AppError.PlaybackUnavailable,
                failure?.toAppError(),
            )
        } finally {
            http.close()
            engine.close()
        }
    }

    @Test
    fun aQueryThatMatchesNothingIsAnEmptyResultRatherThanAFailure(): Unit = runBlocking {
        val engine = OkHttp.create()
        val http = createMusicHttpClient(engine)
        try {
            // 결과가 없는 것은 오류가 아니다. 실패로 바뀌면 화면이 잘못된 문구를 보여준다.
            val result = ProviderASearch(ProviderAClient(http)).search(NONSENSE_QUERY)

            assertTrue("결과 없음이 실패로 처리됐다: ${result.exceptionOrNull()}", result.isSuccess)
            println("의미 없는 검색어 결과 수: ${result.getOrThrow().size}")
        } finally {
            http.close()
            engine.close()
        }
    }

    private companion object {
        /** 형식은 맞지만 존재하지 않는 ID. */
        const val UNKNOWN_TRACK_ID = "zzzzzzzzzzz"
        const val NONSENSE_QUERY = "qxzjvwhkpm 존재하지않는검색어 8471"
    }
}
