package com.keuney.music.feature.library

import androidx.annotation.StringRes
import com.keuney.music.R

/**
 * 라이브러리에서 곡 목록을 담는 구획. 전체 목록을 따로 보여줄 수 있는 것만 여기 있다.
 *
 * 재생목록 구획은 빠져 있다. 줄마다 곡이 아니라 재생목록이 오고 각 줄이 짧아 요약 화면에서
 * 전부 보여도 아래 구획을 밀어내지 않는다.
 */
internal enum class LibrarySection(val route: String, @param:StringRes val titleRes: Int) {
    Recent("recent", R.string.library_recent),
    Favorites("favorites", R.string.library_favorites),
    ;

    companion object {
        fun of(route: String?): LibrarySection? = entries.firstOrNull { it.route == route }
    }
}

/** 요약 화면의 한 구획에 보여줄 최대 곡 수. 나머지는 전체 목록에서 본다. */
internal const val LIBRARY_SECTION_PREVIEW = 5
