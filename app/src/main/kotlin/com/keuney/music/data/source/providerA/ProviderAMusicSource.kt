package com.keuney.music.data.source.providerA

import com.keuney.music.core.model.PlayableStream
import com.keuney.music.core.model.Track
import com.keuney.music.data.source.MusicSource
import javax.inject.Inject

/**
 * Provider A 구현을 MusicSource 계약 뒤에 묶는다. 공급자 DTO는 이 패키지 밖으로 나가지 않는다.
 * getTrack/getRelated는 아직 구현한 작업이 없어 안전한 실패를 반환한다.
 */
internal class ProviderAMusicSource @Inject constructor(
    private val searchApi: ProviderASearch,
    private val streamApi: ProviderAStreamResolver,
) : MusicSource {
    override suspend fun search(query: String): Result<List<Track>> = searchApi.search(query)

    override suspend fun resolveStream(trackId: String): Result<PlayableStream> =
        streamApi.resolveStream(trackId)

    override suspend fun getTrack(trackId: String): Result<Track> =
        Result.failure(ProviderAUnsupportedException())

    override suspend fun getRelated(trackId: String): Result<List<Track>> =
        Result.failure(ProviderAUnsupportedException())
}

/** Messages are fixed app strings and never include provider response content. */
internal class ProviderAUnsupportedException : Exception("Source operation not available")
