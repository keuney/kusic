package com.keuney.music.feature.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** KM-116: 구획 이름을 목적지 인자로 주고받는다. */
class LibrarySectionTest {
    @Test
    fun eachSectionRoundTripsThroughItsRoute() {
        LibrarySection.entries.forEach { section ->
            assertEquals(section, LibrarySection.of(section.route))
        }
    }

    @Test
    fun anUnknownRouteIsNotASection() {
        // 목적지 인자는 문자열이라 무엇이든 올 수 있다. 화면은 이때 기본 구획을 보여준다.
        assertNull(LibrarySection.of("playlists"))
        assertNull(LibrarySection.of(""))
        assertNull(LibrarySection.of(null))
    }

    @Test
    fun theSummaryShowsAFewItemsPerSection() {
        // 전부 쏟으면 아래 구획이 화면 밖으로 밀린다.
        assertEquals(5, LIBRARY_SECTION_PREVIEW)
        assertEquals(listOf(1, 2, 3, 4, 5), (1..12).toList().take(LIBRARY_SECTION_PREVIEW))
    }
}
