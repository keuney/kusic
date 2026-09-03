package com.keuney.music.core.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * 재생 중 스트림이 만료되거나 거부되면 주소를 다시 해석해 한 번만 이어서 시도한다.
 * 상위 소스를 새로 열면 [TrackStreamResolver]가 새 주소를 해석하므로, 다시 여는 것이 곧 재해석이다.
 * 읽던 위치부터 이어 열기 때문에 처음부터 다시 받지 않는다. 재시도 횟수는 열기 한 번당 [maxRetries]로 제한한다.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
internal class RefreshingDataSource(
    private val upstreamFactory: DataSource.Factory,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
) : DataSource {
    private val listeners = mutableListOf<TransferListener>()
    private var baseSpec: DataSpec? = null
    private var current: DataSource? = null
    private var bytesRead = 0L
    private var retries = 0

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
    }

    override fun open(dataSpec: DataSpec): Long {
        closeCurrent()
        baseSpec = dataSpec
        bytesRead = 0
        retries = 0
        return try {
            openUpstream(dataSpec)
        } catch (failure: IOException) {
            if (!canRetry()) throw failure
            retries++
            openUpstream(dataSpec)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val source = current ?: throw IOException("Source is not open")
        val read = try {
            source.read(buffer, offset, length)
        } catch (failure: IOException) {
            if (!canRetry()) throw failure
            retries++
            reopenFromCurrentPosition()
            // 이어 연 뒤의 실패는 그대로 올려 보낸다. 무한 재시도를 만들지 않는다.
            requireNotNull(current).read(buffer, offset, length)
        }
        if (read != C.RESULT_END_OF_INPUT) bytesRead += read
        return read
    }

    override fun getUri(): Uri? = current?.uri ?: baseSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders ?: emptyMap()

    override fun close() {
        closeCurrent()
        baseSpec = null
        bytesRead = 0
        retries = 0
    }

    private fun canRetry(): Boolean = retries < maxRetries && baseSpec != null

    /** 읽은 만큼 건너뛴 지점부터 다시 연다. 새 상위 소스라 주소를 새로 해석한다. */
    private fun reopenFromCurrentPosition() {
        val spec = requireNotNull(baseSpec)
        closeCurrent()
        openUpstream(if (bytesRead > 0) spec.subrange(bytesRead) else spec)
    }

    private fun openUpstream(dataSpec: DataSpec): Long {
        val source = upstreamFactory.createDataSource()
        listeners.forEach(source::addTransferListener)
        current = source
        return try {
            source.open(dataSpec)
        } catch (failure: IOException) {
            closeCurrent()
            throw failure
        }
    }

    private fun closeCurrent() {
        val source = current
        current = null
        runCatching { source?.close() }
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = RefreshingDataSource(upstreamFactory, maxRetries)
    }

    companion object {
        /** PRD 22의 정책: 첫 실패에서 재해석 후 1회만 재시도하고 다시 실패하면 종점 오류다. */
        const val DEFAULT_MAX_RETRIES = 1
    }
}
