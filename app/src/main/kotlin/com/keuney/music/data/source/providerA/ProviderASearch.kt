package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.Track
import com.keuney.music.data.source.providerA.mapper.mapSearchResponse
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
        } catch (_: Exception) {
            Result.failure(ProviderASearchException())
        }
    }
}

internal class ProviderASearchException : Exception("Source search unavailable")
