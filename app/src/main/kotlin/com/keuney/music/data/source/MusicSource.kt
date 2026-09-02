package com.keuney.music.data.source

import com.keuney.music.core.model.PlayableStream
import com.keuney.music.core.model.Track

/** Provider boundary. Implementations must propagate coroutine cancellation. */
interface MusicSource {
    suspend fun search(query: String): Result<List<Track>>
    suspend fun getTrack(trackId: String): Result<Track>
    suspend fun resolveStream(trackId: String): Result<PlayableStream>
    suspend fun getRelated(trackId: String): Result<List<Track>>
}
