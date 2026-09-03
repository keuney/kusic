package com.keuney.music.core.player

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.keuney.music.core.model.AppError
import com.keuney.music.data.source.toAppError
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
            .getOrElse { throw it.toResolveException() }
        return dataSpec.withUri(Uri.parse(stream.url)).withAdditionalHeaders(stream.requestHeaders)
    }
}

/**
 * 해석 실패를 재생 계층이 읽을 수 있는 오류로 바꾼다.
 *
 * 코드만 넘기고 원문 예외와 메시지는 여기서 끊는다(AGENTS.md 12·13). 코드를 굳이 붙이는 이유는
 * 연결이 끊겨서 못 푼 것과 곡 자체를 못 가져오는 것이 재생 쪽에서 같은 입출력 오류로 보이기
 * 때문이다. 구분이 없으면 연결이 돌아와도 이어 붙일지 판단할 수 없다([PlaybackFailure]).
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
private fun Throwable.toResolveException(): IOException = DataSourceException(
    when (toAppError()) {
        AppError.Network -> PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        else -> PlaybackException.ERROR_CODE_IO_UNSPECIFIED
    },
)

/**
 * 사용자가 켠 WiFi 전용 재생 때문에 새 요청을 막았다. 메시지는 고정 문자열이다.
 *
 * 네트워크 실패가 아니다. 연결이 돌아와도 요금제가 그대로면 다시 막힌다. 자동으로 다시 시도할
 * 대상이 되지 않도록 분류하지 않은 입출력 오류로 남긴다.
 */
internal class MeteredNetworkBlockedException : IOException("Metered network playback is disabled")
