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
) : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val trackId = TrackUri.trackIdOrNull(dataSpec.uri.toString()) ?: return dataSpec
        val stream = runBlocking { streamResolver.resolve(trackId) }
            .getOrElse { throw IOException("Stream unavailable") }
        return dataSpec.withUri(Uri.parse(stream.url)).withAdditionalHeaders(stream.requestHeaders)
    }
}
