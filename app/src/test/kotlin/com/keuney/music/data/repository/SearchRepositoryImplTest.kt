package com.keuney.music.data.repository

import com.keuney.music.core.model.AppError
import com.keuney.music.core.model.AppErrorException
import com.keuney.music.core.model.PlayableStream
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import com.keuney.music.data.source.MusicSource
import com.keuney.music.data.source.SourceFailure
import com.keuney.music.data.source.SourceFailureAware
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** KM-070: MusicSource 주입, 오류 매핑, 취소 전파를 확인한다. */
class SearchRepositoryImplTest {
    @Test
    fun successfulSearchPassesTracksThroughUnchanged(): Unit = runBlocking {
        val tracks = listOf(track("a"), track("b"))
        val source = RecordingSource(Result.success(tracks))
        val repository = SearchRepositoryImpl(source)

        val result = repository.search("아이유")

        assertEquals(tracks, result.getOrThrow())
        assertEquals("검색어를 그대로 전달해야 한다", listOf("아이유"), source.queries)
    }

    @Test
    fun everySourceFailureBecomesADomainError(): Unit = runBlocking {
        val cases = mapOf(
            SourceFailure.Network to AppError.Network,
            SourceFailure.Parse to AppError.SourceUnavailable,
            SourceFailure.NotFound to AppError.PlaybackUnavailable,
            SourceFailure.Restricted to AppError.PlaybackUnavailable,
            SourceFailure.Unknown to AppError.Unknown,
        )
        for ((failure, expected) in cases) {
            val repository = SearchRepositoryImpl(RecordingSource(Result.failure(sourceError(failure))))

            val error = repository.search("아이유").exceptionOrNull()

            assertTrue("도메인 오류로 감싸야 한다: $failure", error is AppErrorException)
            assertEquals("$failure 매핑이 다르다", expected, (error as AppErrorException).error)
        }
    }

    @Test
    fun infrastructureFailuresAreMappedToo(): Unit = runBlocking {
        val repository = SearchRepositoryImpl(RecordingSource(Result.failure(IOException("소켓"))))

        val error = repository.search("아이유").exceptionOrNull()

        assertEquals(AppError.Network, (error as AppErrorException).error)
    }

    @Test
    fun thrownFailuresAreMappedInsteadOfEscaping(): Unit = runBlocking {
        val repository = SearchRepositoryImpl(ThrowingSource(IllegalStateException("원문 노출 금지")))

        val error = repository.search("아이유").exceptionOrNull()

        assertTrue(error is AppErrorException)
        assertEquals(AppError.Unknown, (error as AppErrorException).error)
    }

    @Test
    fun theOriginalMessageNeverReachesTheCaller(): Unit = runBlocking {
        val secret = "sensitive-test-value"
        val repository = SearchRepositoryImpl(RecordingSource(Result.failure(IllegalStateException(secret))))

        val error = requireNotNull(repository.search("아이유").exceptionOrNull())

        assertFalse(error.message.orEmpty().contains(secret))
        assertFalse(error.toString().contains(secret))
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotTurnedIntoAFailure(): Unit = runBlocking {
        SearchRepositoryImpl(ThrowingSource(CancellationException("cancelled"))).search("아이유")
    }

    private fun track(id: String) = Track(id, "제목 $id", "아티스트", null, 180_000, SourceType.Remote)

    private fun sourceError(failure: SourceFailure): Throwable =
        object : Exception("고정 문자열"), SourceFailureAware {
            override val failure: SourceFailure = failure
        }

    private open class RecordingSource(private val result: Result<List<Track>>) : MusicSource {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String): Result<List<Track>> {
            queries += query
            return result
        }

        override suspend fun getTrack(trackId: String): Result<Track> =
            Result.failure(UnsupportedOperationException())

        override suspend fun resolveStream(trackId: String): Result<PlayableStream> =
            Result.failure(UnsupportedOperationException())

        override suspend fun getRelated(trackId: String): Result<List<Track>> =
            Result.failure(UnsupportedOperationException())
    }

    private class ThrowingSource(private val failure: Throwable) : RecordingSource(Result.success(emptyList())) {
        override suspend fun search(query: String): Result<List<Track>> = throw failure
    }
}
