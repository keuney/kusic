package com.keuney.music.core.player

import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KM-139 인수 조건 중 목록에 관한 것: 곡이 빠지지 않고 겹치지 않으며 순서가 고정되어 있지 않다.
 *
 * 씨앗을 주어 순서를 고정한다. 무작위를 검사하려면 결과가 같아야 하기 때문이다. 실제 재생에서는
 * 씨앗 없는 [Random.Default]를 쓴다.
 */
class ShuffledQueueTest {
    @Test
    fun everyTrackSurvivesAndNoneIsDuplicated() {
        val tracks = tracks(20)

        val shuffled = shuffledQueue(tracks, Random(1))

        assertEquals("곡 수가 달라졌다", tracks.size, shuffled.size)
        assertEquals("같은 곡이 두 번 들어갔거나 빠졌다", tracks.toSet(), shuffled.toSet())
    }

    @Test
    fun theOrderIsNotTheOrderItCameIn() {
        val tracks = tracks(20)

        assertNotEquals("섞이지 않았다", tracks, shuffledQueue(tracks, Random(1)))
    }

    @Test
    fun differentDrawsGiveDifferentOrders() {
        val tracks = tracks(20)

        // 스무 곡의 순서가 우연히 같을 확률은 20!의 역수다. 씨앗을 고정했으므로 이 판정은
        // 실행마다 같다.
        assertNotEquals(
            "다시 눌렀는데 같은 순서가 나온다",
            shuffledQueue(tracks, Random(1)),
            shuffledQueue(tracks, Random(2)),
        )
    }

    @Test
    fun theSameDrawGivesTheSameOrder() {
        val tracks = tracks(20)

        // 검사가 순서를 고정할 수 있다는 뜻이다. 이 성질이 없으면 위의 판정들을 믿을 수 없다.
        assertEquals(
            shuffledQueue(tracks, Random(7)),
            shuffledQueue(tracks, Random(7)),
        )
    }

    @Test
    fun theListItWasGivenIsNotChanged() {
        val tracks = tracks(20)
        val before = tracks.toList()

        shuffledQueue(tracks, Random(1))

        // 화면이 관찰 중인 목록을 그대로 넘긴다. 그것을 뒤섞으면 목록의 줄 순서가 함께 바뀐다.
        assertEquals("받은 목록이 바뀌었다", before, tracks)
    }

    @Test
    fun anEmptyListStaysEmpty() {
        assertTrue(shuffledQueue(emptyList(), Random(1)).isEmpty())
    }

    @Test
    fun oneTrackIsTheOnlyPossibleOrder() {
        val tracks = tracks(1)

        assertEquals(tracks, shuffledQueue(tracks, Random(1)))
    }

    private fun tracks(count: Int): List<Track> = (1..count).map {
        Track(
            id = "shuffle-$it",
            title = "곡 $it",
            artist = "Keuney Music",
            artworkUrl = null,
            durationMs = null,
            source = SourceType.Remote,
        )
    }
}
