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
    suspend fun resolveStream(trackId: String): Result<PlayableStream> =
        resolveStream(trackId, ProviderAConfig.streamCandidates)

    /** 후보 클라이언트를 순서대로 시도하고 첫 성공을 반환한다. 모두 실패하면 마지막 실패를 반환한다. */
    suspend fun resolveStream(
        trackId: String,
        profiles: List<ProviderAClientProfile>,
    ): Result<PlayableStream> {
        require(profiles.isNotEmpty()) { "At least one client profile is required" }
        if (trackId.isBlank()) return Result.failure(ProviderAStreamException("Track ID is required"))
        var last: Result<PlayableStream>? = null
        for (profile in profiles) {
            val attempt = resolveWith(trackId, profile)
            if (attempt.isSuccess) return attempt
            last = attempt
        }
        return checkNotNull(last)
    }

    suspend fun resolveWith(
        trackId: String,
        profile: ProviderAClientProfile,
    ): Result<PlayableStream> {
        if (trackId.isBlank()) return Result.failure(ProviderAStreamException("Track ID is required"))
        return try {
            val response = client.request("player", playerFields(trackId, profile), profile)
            Result.success(mapStreamResponse(response, Instant.now()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: ProviderAStreamException) {
            Result.failure(failure)
        } catch (_: Exception) {
            Result.failure(ProviderAStreamException("Stream request failed"))
        }
    }

    private fun playerFields(trackId: String, profile: ProviderAClientProfile) = buildJsonObject {
        put("videoId", trackId)
        put("contentCheckOk", true)
        put("racyCheckOk", true)
        if (profile.sendsSignatureTimestamp) {
            putJsonObject("playbackContext") {
                putJsonObject("contentPlaybackContext") {
                    put("signatureTimestamp", ProviderAConfig.signatureTimestamp)
                }
            }
        }
    }
}

/** Messages are fixed app strings and never include provider response content. */
internal class ProviderAStreamException(message: String) : Exception(message)
