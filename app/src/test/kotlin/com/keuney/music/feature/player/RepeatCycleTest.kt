package com.keuney.music.feature.player

import com.keuney.music.core.player.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** KM-096: 버튼 하나로 세 상태를 돈다. */
class RepeatCycleTest {
    @Test
    fun theButtonGoesOffThenAllThenOne() {
        assertEquals(RepeatMode.All, RepeatMode.Off.next())
        assertEquals(RepeatMode.One, RepeatMode.All.next())
        assertEquals(RepeatMode.Off, RepeatMode.One.next())
    }

    @Test
    fun threePressesComeBackToWhereItStarted() {
        RepeatMode.entries.forEach { start ->
            assertEquals(start, start.next().next().next())
        }
    }

    @Test
    fun everyModeIsReachable() {
        val visited = mutableSetOf(RepeatMode.Off)
        var mode = RepeatMode.Off
        repeat(RepeatMode.entries.size) {
            mode = mode.next()
            visited += mode
        }
        assertEquals(RepeatMode.entries.toSet(), visited)
    }
}
