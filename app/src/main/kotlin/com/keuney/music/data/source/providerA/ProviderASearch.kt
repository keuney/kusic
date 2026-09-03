package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.Track
import com.keuney.music.data.source.SourceFailure
import com.keuney.music.data.source.SourceFailureAware
import com.keuney.music.data.source.httpStatusToFailure
import com.keuney.music.data.source.providerA.mapper.mapSearchResponse
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

internal class ProviderASearch @Inject constructor(private val client: ProviderAClient) {
    suspend fun search(query: String): Result<List<Track>> {
        if (query.isBlank()) return Result.success(emptyList())
        return try {
            val response = client.request("search", buildJsonObject {
                put("query", query.trim())
            })
            Result.success(mapSearchResponse(response))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (rejected: ResponseException) {
            Result.failure(ProviderASearchException(httpStatusToFailure(rejected.response.status.value)))
        } catch (_: IllegalStateException) {
            // mapper의 구조 검사 실패. 소스가 응답 형식을 바꾼 경우다.
            Result.failure(ProviderASearchException(SourceFailure.Parse))
        } catch (_: Exception) {
            Result.failure(ProviderASearchException(SourceFailure.Network))
        }
    }
}

internal class ProviderASearchException(
    override val failure: SourceFailure = SourceFailure.Unknown,
) : Exception("Source search unavailable"), SourceFailureAware
