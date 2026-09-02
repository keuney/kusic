package com.keuney.music.core.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 공급자를 흉내 낸 상위 소스로 청크 분할 동작을 검증한다.
 * 상위 소스는 열린 Range와 상한을 넘는 요청을 거부하므로, 통과 자체가 요청 형태의 증거다.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class ChunkedHttpDataSourceTest {
    @Test
    fun readsTheWholeStreamAcrossChunkBoundaries() {
        val content = ByteArray(5_000) { (it % 251).toByte() }
        val factory = CountingFactory(content, maxLength = CHUNK)
        val source = ChunkedHttpDataSource(factory, CHUNK)

        val total = source.open(DataSpec.Builder().setUri(URI).build())

        assertEquals(content.size.toLong(), total)
        assertArrayEquals(content, source.readFully(content.size))
        assertEquals(C.RESULT_END_OF_INPUT, source.read(ByteArray(16), 0, 16))
        assertEquals(5, factory.created)
        source.close()
    }

    @Test
    fun startsFromTheRequestedPositionAndReportsTheRemainingLength() {
        val content = ByteArray(2_500) { (it % 251).toByte() }
        val factory = CountingFactory(content, maxLength = CHUNK)
        val source = ChunkedHttpDataSource(factory, CHUNK)

        val remaining = source.open(DataSpec.Builder().setUri(URI).setPosition(1_500).build())

        assertEquals(1_000L, remaining)
        assertArrayEquals(content.copyOfRange(1_500, 2_500), source.readFully(1_000))
        source.close()
    }

    @Test
    fun keepsAnExplicitLengthWithinTheChunkLimit() {
        val content = ByteArray(5_000) { (it % 251).toByte() }
        val factory = CountingFactory(content, maxLength = CHUNK)
        val source = ChunkedHttpDataSource(factory, CHUNK)

        val length = source.open(DataSpec.Builder().setUri(URI).setLength(2_500).build())

        assertEquals(2_500L, length)
        assertArrayEquals(content.copyOfRange(0, 2_500), source.readFully(2_500))
        assertEquals(C.RESULT_END_OF_INPUT, source.read(ByteArray(16), 0, 16))
        assertEquals(3, factory.created)
        source.close()
    }

    private fun DataSource.readFully(count: Int): ByteArray {
        val buffer = ByteArray(count)
        var filled = 0
        while (filled < count) {
            val read = read(buffer, filled, minOf(300, count - filled))
            if (read == C.RESULT_END_OF_INPUT) break
            filled += read
        }
        assertEquals("요청한 만큼 읽지 못함", count, filled)
        return buffer
    }

    @androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
    private class CountingFactory(
        private val content: ByteArray,
        private val maxLength: Long,
    ) : DataSource.Factory {
        var created = 0
            private set

        override fun createDataSource(): DataSource {
            created++
            return BoundedRangeSource(content, maxLength)
        }
    }

    /** 열린 Range와 상한 초과 요청을 거부하는 상위 소스. */
    @androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
    private class BoundedRangeSource(
        private val content: ByteArray,
        private val maxLength: Long,
    ) : DataSource {
        private var uri: Uri? = null
        private var position = 0
        private var end = 0

        override fun addTransferListener(transferListener: TransferListener) = Unit

        override fun open(dataSpec: DataSpec): Long {
            if (dataSpec.length == C.LENGTH_UNSET.toLong()) throw IOException("열린 Range 거부")
            if (dataSpec.length > maxLength) throw IOException("상한 초과 요청 거부")
            uri = dataSpec.uri
            position = dataSpec.position.toInt()
            end = minOf(content.size.toLong(), dataSpec.position + dataSpec.length).toInt()
            return (end - position).toLong()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= end) return C.RESULT_END_OF_INPUT
            val count = minOf(length, end - position)
            content.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }

        override fun getUri(): Uri? = uri

        override fun getResponseHeaders(): Map<String, List<String>> =
            mapOf("Content-Range" to listOf("bytes $position-${end - 1}/${content.size}"))

        override fun close() = Unit
    }

    private companion object {
        const val CHUNK = 1_000L
        val URI: Uri = Uri.parse("https://example.invalid/audio")
    }
}
