package com.keuney.music.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.keuney.music.R

/** 하단 내비게이션의 세 목적지(PRD 35). 순서가 화면에 나오는 순서다. */
internal enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home("home", R.string.destination_home, Icons.Filled.Home),
    Search("search", R.string.destination_search, Icons.Filled.Search),
    Library("library", R.string.destination_library, Icons.Filled.List),
}

/** 하단 내비게이션에 없는 전체 화면 목적지. 미니 플레이어를 눌러 들어온다. */
internal const val NOW_PLAYING_ROUTE = "now-playing"

/** 전체 화면 플레이어의 대기열 버튼으로 들어온다. */
internal const val QUEUE_ROUTE = "queue"

/** 라이브러리의 재생목록을 눌러 들어온다. */
internal const val PLAYLIST_ID_ARG = "playlistId"
internal const val PLAYLIST_ROUTE = "playlist/{$PLAYLIST_ID_ARG}"

internal fun playlistRoute(playlistId: Long) = "playlist/$playlistId"
