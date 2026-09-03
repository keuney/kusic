package com.keuney.music.ui.format

/**
 * 재생 위치와 곡 길이를 화면에 쓰는 `분:초`로 만든다. 플레이어와 검색 결과가 같은 표기를 쓰도록
 * 한곳에 둔다. 시간 단위는 v0.1 대상 곡 길이에 필요하지 않아 분으로 계속 센다.
 */
internal fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
