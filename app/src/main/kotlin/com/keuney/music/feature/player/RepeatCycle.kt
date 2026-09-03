package com.keuney.music.feature.player

import com.keuney.music.core.player.RepeatMode

/**
 * 반복 버튼을 한 번 누를 때 다음 모드.
 *
 * 없음 → 전체 → 한 곡 → 없음 순서다. 여러 곡을 이어 듣는 쪽이 한 곡 반복보다 흔하므로 먼저 온다.
 */
internal fun RepeatMode.next(): RepeatMode = when (this) {
    RepeatMode.Off -> RepeatMode.All
    RepeatMode.All -> RepeatMode.One
    RepeatMode.One -> RepeatMode.Off
}
