package com.keuney.music.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

/** 설정 화면이 캐시 크기를 보여주는 방식. 고른 상한과 보이는 값이 어긋나지 않아야 한다. */
class ByteFormatTest {
    @Test
    fun cacheLimitsReadBackAsTheNumberOnTheButton() {
        assertEquals("128MB", formatBytes(128L * 1024 * 1024))
        assertEquals("256MB", formatBytes(256L * 1024 * 1024))
        assertEquals("512MB", formatBytes(512L * 1024 * 1024))
    }

    @Test
    fun smallSizesAreShownInKilobytes() {
        assertEquals("0KB", formatBytes(0))
        assertEquals("0KB", formatBytes(1023))
        assertEquals("1KB", formatBytes(1024))
        assertEquals("1023KB", formatBytes(1024L * 1024 - 1))
    }

    @Test
    fun gigabytesKeepOneDecimal() {
        assertEquals("1.0GB", formatBytes(1024L * 1024 * 1024))
        assertEquals("1.5GB", formatBytes(1536L * 1024 * 1024))
    }

    @Test
    fun negativeSizesAreTreatedAsEmpty() {
        // cacheSpace가 음수를 줄 일은 없지만, 화면에 "-1KB"가 나오는 것보다 낫다.
        assertEquals("0KB", formatBytes(-5))
    }
}
