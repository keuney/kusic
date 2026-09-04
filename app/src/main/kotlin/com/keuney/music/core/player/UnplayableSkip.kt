package com.keuney.music.core.player

/**
 * 재생할 수 없는 곡에서 다음 곡으로 넘어갈지 정한다(KM-138).
 *
 * 플레이어를 만지지 않고 판단만 한다. 실제로 넘기는 일은 재생을 소유한 [MusicService]가 한다.
 * 규칙을 떼어 둔 이유는 기기 없이 검사할 수 있어야 하기 때문이다([NetworkRecovery]와 같다).
 *
 * 넘어가는 경우는 하나뿐이다. **곡 자체를 가져올 수 없고, 듣던 중이었고, 대기열에 다음 곡이
 * 있을 때.** 나머지는 넘어가지 않는다.
 */
internal class UnplayableSkip {
    /** 한 번이라도 재생되기 전까지 연달아 넘긴 횟수. */
    private var consecutive = 0

    /** 재생이 시작됐다. 넘기기는 여기서 끝나므로 횟수를 처음으로 되돌린다. */
    fun onPlayed() {
        consecutive = 0
    }

    /**
     * 지금 오류에서 다음 곡으로 넘어갈지.
     *
     * [queueSize]만큼 넘긴 뒤에는 넘어가지 않는다. 대기열을 한 번 훑었다는 뜻이다. 반복이
     * 전체 반복이면 마지막 곡 뒤에 처음으로 돌아오므로 "다음 곡이 있다"가 늘 참이 되고, 모든
     * 곡이 재생 불가일 때 끝없이 돌 수 있다. 그것을 막는 것이 이 횟수의 유일한 목적이다.
     */
    fun shouldSkip(
        failure: PlaybackFailure?,
        playWhenReady: Boolean,
        hasNext: Boolean,
        queueSize: Int,
    ): Boolean {
        // 네트워크 때문이면 기다린다(연결이 돌아오면 그 곡을 이어 듣는다, ADR-063).
        // 설정이 막은 것이면 곡의 문제가 아니므로 넘어가지 않는다.
        if (failure != PlaybackFailure.Source) return false
        // 멈춰 둔 재생을 넘겨 가며 이어 트는 것은 회복이 아니라 참견이다.
        if (!playWhenReady) return false
        // 마지막 곡이면 넘어갈 곳이 없다. 그때는 문구와 다시 시도가 남는다.
        if (!hasNext) return false
        if (consecutive >= queueSize) return false
        consecutive++
        return true
    }
}
