package com.keuney.music.core.player

import com.keuney.music.core.model.PlayableStream
import com.keuney.music.data.source.MusicSource
import javax.inject.Inject

/**
 * 재생에 필요한 시점에만 Track ID를 스트림으로 바꾼다. 결과는 호출자에게만 전달하며
 * 보관하거나 캐시하지 않는다. 만료 후 재해석과 재시도 정책은 KM-061에서 이 자리에 붙인다.
 */
internal class StreamResolver @Inject constructor(private val source: MusicSource) {
    suspend fun resolve(trackId: String): Result<PlayableStream> = source.resolveStream(trackId)
}
