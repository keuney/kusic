package com.keuney.music.data.repository

import com.keuney.music.core.model.AppErrorException
import com.keuney.music.core.model.Track
import com.keuney.music.core.search.SearchRepository
import com.keuney.music.data.source.MusicSource
import com.keuney.music.data.source.toAppError
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

internal class SearchRepositoryImpl @Inject constructor(
    private val source: MusicSource,
) : SearchRepository {
    override suspend fun search(query: String): Result<List<Track>> = try {
        source.search(query).fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(AppErrorException(it.toAppError())) },
        )
    } catch (cancelled: CancellationException) {
        // 취소는 오류가 아니다. 화면이 실패 문구를 띄우지 않도록 그대로 전파한다.
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(AppErrorException(failure.toAppError()))
    }
}
