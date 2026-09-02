package com.keuney.music.core.player

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * ExoPlayer가 자리표시 URI를 열기 직전에 실제 스트림 주소로 바꾼다.
 * 로딩 스레드에서 호출되므로 해석을 블로킹으로 기다린다.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
internal class TrackStreamResolver @Inject constructor(
    private val streamResolver: StreamResolver,
    private val networkPolicy: NetworkPolicy,
) : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val trackId = TrackUri.trackIdOrNull(dataSpec.uri.toString()) ?: return dataSpec
        // 캐시에 있는 구간은 여기까지 오지 않으므로 제한을 켜도 그대로 재생된다.
        if (runBlocking { networkPolicy.blocksRemoteFetch() }) {
            throw MeteredNetworkBlockedException()
        }
        val stream = runBlocking { streamResolver.resolve(trackId) }
            .getOrElse { throw IOException("Stream unavailable") }
        return dataSpec.withUri(Uri.parse(stream.url)).withAdditionalHeaders(stream.requestHeaders)
    }
}

/** 사용자가 켠 WiFi 전용 재생 때문에 새 요청을 막았다. 메시지는 고정 문자열이다. */
internal class MeteredNetworkBlockedException : IOException("Metered network playback is disabled")
