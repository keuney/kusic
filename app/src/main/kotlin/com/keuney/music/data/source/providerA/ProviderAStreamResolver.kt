package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.PlayableStream
import com.keuney.music.data.source.providerA.mapper.mapStreamResponse
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal class ProviderAStreamResolver @Inject constructor(private val client: ProviderAClient) {
    suspend fun resolveStream(trackId: String): Result<PlayableStream> {
        if (trackId.isBlank()) return Result.failure(ProviderAStreamException("Track ID is required"))
        return try {
            val response = client.request("player", buildJsonObject {
                put("videoId", trackId)
                putJsonObject("playbackContext") {
                    putJsonObject("contentPlaybackContext") {
                        put("signatureTimestamp", ProviderAConfig.signatureTimestamp)
                    }
                }
            })
            Result.success(mapStreamResponse(response, Instant.now()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: ProviderAStreamException) {
            Result.failure(failure)
        } catch (_: Exception) {
            Result.failure(ProviderAStreamException("Stream request failed"))
        }
    }
}

/** Messages are fixed app strings and never include provider response content. */
internal class ProviderAStreamException(message: String) : Exception(message)
