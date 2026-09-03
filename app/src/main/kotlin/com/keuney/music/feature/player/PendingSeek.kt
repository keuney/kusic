package com.keuney.music.feature.player

import kotlin.math.abs

/**
 * 손을 뗀 뒤 아직 실제 재생 위치에 반영되지 않은 탐색.
 *
 * 위치 보고는 250ms마다 오므로, 손을 떼자마자 표시를 실제 위치로 넘기면 슬라이더가 탐색 이전
 * 자리로 한 번 되돌아갔다가 목표로 뛴다. 목표에 도달할 때까지 목표를 보여주어 그 되돌아감을
 * 없앤다.
 *
 * @param fromMs 탐색을 시작한 시점의 실제 위치. 방향과 수렴 여부를 판단하는 데 쓴다.
 */
internal data class PendingSeek(val fromMs: Long, val toMs: Long) {
    /**
     * 표시를 실제 위치에 넘겨도 되는지.
     *
     * 세 경우에 넘긴다. 목표 근처에 왔을 때, 목표를 지나 재생이 계속되고 있을 때, 그리고
     * 목표에서 오히려 멀어졌을 때다. 마지막 경우가 없으면 탐색이 받아들여지지 않았을 때
     * 표시가 목표에 붙어 멈춘다.
     */
    fun isSettled(reportedMs: Long): Boolean = when {
        abs(reportedMs - toMs) <= TOLERANCE_MS -> true
        abs(reportedMs - toMs) > abs(fromMs - toMs) -> true
        toMs > fromMs -> reportedMs > toMs
        else -> reportedMs < toMs
    }

    private companion object {
        /** 위치 보고 간격보다 넉넉히 두어 한 번의 보고로 정리되게 한다. */
        const val TOLERANCE_MS = 1_000L
    }
}

/**
 * 슬라이더와 시간 표시에 쓸 위치.
 *
 * 손가락이 있으면 손가락이 이긴다. 손을 뗀 직후에는 아직 도달하지 않은 목표를, 그 밖에는
 * 실제 재생 위치를 쓴다.
 */
internal fun seekDisplayPositionMs(reportedMs: Long, draggingMs: Long?, pending: PendingSeek?): Long = when {
    draggingMs != null -> draggingMs
    pending != null && !pending.isSettled(reportedMs) -> pending.toMs
    else -> reportedMs
}
