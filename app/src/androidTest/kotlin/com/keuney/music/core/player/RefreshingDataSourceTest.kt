package com.keuney.music.core.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * KM-061 인수 조건: 첫 실패 → 재해석 → 1회 재시도 → 두 번째 실패는 종점 오류, 무한 재시도 없음.
 * 상위 소스를 새로 여는 것이 곧 재해석이므로 생성 횟수로 재해석을 확인한다.
 */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class RefreshingDataSourceTest {
    @Test
    fun aFailureWhileOpeningIsRetriedOnceWithAFreshUpstream() {
        val factory = ScriptedFactory(content, failOpenTimes = 1)
        val source = RefreshingDataSource(factory)

        val length = source.open(spec())

        assertEquals(content.size.toLong(), length)
        assertArrayEquals(content, source.readFully(content.size))
        assertEquals("재해석을 위해 상위 소스를 다시 열어야 한다", 2, factory.created)
        source.close()
    }

    @Test
    fun aFailureWhilePlayingResumesFromThePositionAlreadyRead() {
        val factory = ScriptedFactory(content, failReadAfter = 1_500)
        val source = RefreshingDataSource(factory)

        source.open(spec())
        val received = source.readFully(content.size)

        assertArrayEquals("이어 받은 내용이 원본과 달라짐", content, received)
        assertEquals(2, factory.created)
        // 두 번째 소스는 이미 읽은 지점부터 열려야 한다. 처음부터 다시 받지 않는다.
        assertEquals(1_500L, factory.lastOpenPosition)
        source.close()
    }

    @Test
    fun aSecondFailureIsTerminalAndDoesNotRetryForever() {
        val factory = ScriptedFactory(content, failOpenTimes = Int.MAX_VALUE)
        val source = RefreshingDataSource(factory)

        try {
            source.open(spec())
            fail("두 번째 실패는 종점 오류여야 한다")
        } catch (expected: IOException) {
            assertTrue(expected is HttpDataSource.InvalidResponseCodeException)
        }
        assertEquals("재시도는 한 번뿐이어야 한다", 2, factory.created)
        source.close()
    }

    @Test
    fun aSecondFailureWhilePlayingIsTerminal() {
        val factory = ScriptedFactory(content, failReadAfter = 1_000, failEveryRead = true)
        val source = RefreshingDataSource(factory)
        source.open(spec())

        try {
            source.readFully(content.size)
            fail("이어 연 뒤의 실패는 종점 오류여야 한다")
        } catch (_: IOException) {
            // 기대한 동작
        }
        assertEquals("재시도는 한 번뿐이어야 한다", 2, factory.created)
        source.close()
    }

    @Test
    fun aHealthyStreamNeverReopens() {
        val factory = ScriptedFactory(content)
        val source = RefreshingDataSource(factory)

        source.open(spec())
        assertArrayEquals(content, source.readFully(content.size))

        assertEquals(1, factory.created)
        source.close()
    }

    private val content = ByteArray(4_000) { (it % 251).toByte() }

    private fun spec() = DataSpec.Builder().setUri(URI).build()

    private fun DataSource.readFully(count: Int): ByteArray {
        val buffer = ByteArray(count)
        var filled = 0
        while (filled < count) {
            val read = read(buffer, filled, minOf(256, count - filled))
            if (read == C.RESULT_END_OF_INPUT) break
            filled += read
        }
        assertEquals("요청한 만큼 읽지 못함", count, filled)
        return buffer
    }

    /** 열기 실패와 읽기 도중 실패를 지정한 횟수만큼 재현하는 상위 소스. */
    private class ScriptedFactory(
        private val content: ByteArray,
        private val failOpenTimes: Int = 0,
        private val failReadAfter: Long = -1,
        private val failEveryRead: Boolean = false,
    ) : DataSource.Factory {
        var created = 0
            private set
        var lastOpenPosition = -1L
            private set
        private var opensFailed = 0
        private var readFailures = 0

        override fun createDataSource(): DataSource {
            created++
            val shouldFailOpen = opensFailed < failOpenTimes
            if (shouldFailOpen) opensFailed++
            val shouldFailRead = failReadAfter >= 0 && (failEveryRead || readFailures == 0)
            if (shouldFailRead) readFailures++
            return ScriptedSource(
                content = content,
                failOpen = shouldFailOpen,
                failReadAfter = if (shouldFailRead) failReadAfter else -1,
                onOpen = { lastOpenPosition = it },
            )
        }
    }

    private class ScriptedSource(
        private val content: ByteArray,
        private val failOpen: Boolean,
        private val failReadAfter: Long,
        private val onOpen: (Long) -> Unit,
    ) : DataSource {
        private var uri: Uri? = null
        private var position = 0
        private var readSinceOpen = 0L

        override fun addTransferListener(transferListener: TransferListener) = Unit

        override fun open(dataSpec: DataSpec): Long {
            onOpen(dataSpec.position)
            if (failOpen) throw invalidResponse(dataSpec)
            uri = dataSpec.uri
            position = dataSpec.position.toInt()
            readSinceOpen = 0
            return (content.size - position).toLong()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (failReadAfter >= 0 && readSinceOpen >= failReadAfter) {
                throw IOException("스트림이 끊겼다")
            }
            if (position >= content.size) return C.RESULT_END_OF_INPUT
            val allowed = minOf(length.toLong(), content.size - position.toLong())
            val capped = if (failReadAfter >= 0) minOf(allowed, failReadAfter - readSinceOpen) else allowed
            val count = capped.toInt().coerceAtLeast(1)
            content.copyInto(buffer, offset, position, position + count)
            position += count
            readSinceOpen += count
            return count
        }

        override fun getUri(): Uri? = uri

        override fun close() = Unit

        private fun invalidResponse(dataSpec: DataSpec) = HttpDataSource.InvalidResponseCodeException(
            403,
            "Forbidden",
            null,
            emptyMap(),
            dataSpec,
            ByteArray(0),
        )
    }

    private companion object {
        val URI: Uri = Uri.parse("keuney://track/gdZLi9oWNZg")
    }
}
