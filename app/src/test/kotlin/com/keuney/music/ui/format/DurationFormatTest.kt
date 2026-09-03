package com.keuney.music.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {
    @Test
    fun zeroIsMinuteAndPaddedSecond() {
        assertEquals("0:00", formatDuration(0))
    }

    @Test
    fun secondUnderTenKeepsTwoDigits() {
        assertEquals("0:07", formatDuration(7_400))
    }

    @Test
    fun secondIsTruncatedNotRounded() {
        assertEquals("0:59", formatDuration(59_999))
    }

    @Test
    fun minuteRollsOverAtSixtySeconds() {
        assertEquals("1:00", formatDuration(60_000))
    }

    @Test
    fun overAnHourKeepsCountingMinutes() {
        assertEquals("75:03", formatDuration(4_503_000))
    }

    @Test
    fun negativeIsShownAsZero() {
        assertEquals("0:00", formatDuration(-5_000))
    }
}
