package com.keuney.music.core.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * 공급자 스트림은 열린 Range나 큰 범위 요청을 거부한다. 하나의 재생 요청을 상한 이하의
 * 닫힌 Range 요청 여러 개로 나눠 순서대로 읽는다. 상위 계층에는 연속된 바이트열로 보인다.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
internal class ChunkedHttpDataSource(
    private val upstreamFactory: DataSource.Factory,
    private val chunkSize: Long,
) : DataSource {
    private val listeners = mutableListOf<TransferListener>()
    private var baseSpec: DataSpec? = null
    private var current: DataSource? = null
    private var position = 0L
    private var bytesRemaining = C.LENGTH_UNSET.toLong()
    private var chunkRemaining = 0L

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
    }

    override fun open(dataSpec: DataSpec): Long {
        close()
        baseSpec = dataSpec
        position = dataSpec.position
        bytesRemaining = dataSpec.length
        openNextChunk()
        if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            val total = totalLengthOrUnset()
            if (total != C.LENGTH_UNSET.toLong()) bytesRemaining = (total - position).coerceAtLeast(0)
        }
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        while (chunkRemaining == 0L) {
            closeCurrentChunk()
            if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
            openNextChunk()
            if (chunkRemaining == 0L) {
                bytesRemaining = 0
                return C.RESULT_END_OF_INPUT
            }
        }
        val allowed = minOf(length.toLong(), chunkRemaining).toInt()
        val read = requireNotNull(current).read(buffer, offset, allowed)
        if (read == C.RESULT_END_OF_INPUT) {
            // 청크가 예고보다 일찍 끝나면 남은 길이를 신뢰하지 않고 스트림 끝으로 본다.
            chunkRemaining = 0
            bytesRemaining = 0
            return C.RESULT_END_OF_INPUT
        }
        position += read
        chunkRemaining -= read
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        return read
    }

    override fun getUri(): Uri? = current?.uri ?: baseSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        current?.responseHeaders ?: emptyMap()

    override fun close() {
        closeCurrentChunk()
        baseSpec = null
        position = 0
        bytesRemaining = C.LENGTH_UNSET.toLong()
    }

    private fun openNextChunk() {
        val spec = baseSpec ?: throw IOException("Chunked source is not open")
        val requested = when (val remaining = bytesRemaining) {
            C.LENGTH_UNSET.toLong() -> chunkSize
            else -> minOf(chunkSize, remaining)
        }
        if (requested <= 0) {
            chunkRemaining = 0
            return
        }
        val source = upstreamFactory.createDataSource()
        listeners.forEach(source::addTransferListener)
        val opened = source.open(
            spec.buildUpon().setPosition(position).setLength(requested).build(),
        )
        current = source
        chunkRemaining = if (opened == C.LENGTH_UNSET.toLong()) requested else opened
    }

    private fun closeCurrentChunk() {
        val source = current
        current = null
        chunkRemaining = 0
        source?.close()
    }

    /** 첫 청크 응답의 Content-Range에서 전체 길이를 읽는다. 없으면 미지정으로 둔다. */
    private fun totalLengthOrUnset(): Long {
        val headers = current?.responseHeaders ?: return C.LENGTH_UNSET.toLong()
        val value = headers.entries
            .firstOrNull { it.key.equals("Content-Range", ignoreCase = true) }
            ?.value?.firstOrNull()
            ?: return C.LENGTH_UNSET.toLong()
        return value.substringAfter('/', "").trim().toLongOrNull() ?: C.LENGTH_UNSET.toLong()
    }

    @androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val chunkSize: Long = DEFAULT_CHUNK_SIZE,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = ChunkedHttpDataSource(upstreamFactory, chunkSize)
    }

    companion object {
        /** 512KB는 통과하고 1MB는 거부되는 것을 실기기에서 확인했다(ADR-033). */
        const val DEFAULT_CHUNK_SIZE = 512L * 1024
    }
}
