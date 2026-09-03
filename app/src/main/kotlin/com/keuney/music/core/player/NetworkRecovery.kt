package com.keuney.music.core.player

/**
 * 네트워크가 끊겨 멈춘 재생을 연결이 돌아왔을 때 이어 붙일지 정한다.
 *
 * 플레이어를 만지지 않고 판단만 한다. 규칙은 실기기 없이도 검사할 수 있어야 하고, 실제로 다시
 * 준비시키는 일은 재생을 소유한 [MusicService]가 한다.
 *
 * 짧은 끊김은 여기까지 오지 않는다. ExoPlayer가 스스로 몇 번 다시 읽어 보고, 그 사이 재생은
 * 버퍼로 이어진다. 여기서 다루는 것은 그 재시도까지 실패해 재생이 멈춰 선 뒤다.
 */
internal class NetworkRecovery(private val maxAttempts: Int = MAX_ATTEMPTS) {
    /** 연결만 돌아오면 이어 붙일 재생이 멈춰 있는가. */
    private var waiting = false

    /** 회복하지 못한 채 쓴 자동 시도 횟수. */
    private var attempts = 0

    /**
     * 재생이 오류로 멈췄다. 네트워크 때문이었고 듣던 중이었을 때만 이어 붙일 자격이 생긴다.
     *
     * 멈춰 둔 재생은 되살리지 않는다. 사용자가 멈춘 것을 네트워크가 돌아왔다고 다시 트는 것은
     * 회복이 아니라 참견이다.
     */
    fun onError(failure: PlaybackFailure?, playWhenReady: Boolean) {
        waiting = failure == PlaybackFailure.Network && playWhenReady
    }

    /**
     * 연결이 돌아왔다. 다시 시도할 차례면 true이고 시도 횟수를 하나 쓴다.
     *
     * 횟수를 제한하는 이유는 연결이 붙었다 끊겼다 하는 곳에서 같은 실패를 끝없이 되풀이하지
     * 않기 위해서다. 한 번이라도 다시 재생되면([onReady]) 횟수는 처음으로 돌아간다.
     */
    fun onNetworkAvailable(): Boolean {
        if (!waiting || attempts >= maxAttempts) return false
        attempts++
        return true
    }

    /** 다시 재생할 수 있게 됐다. 회복했으므로 기다림도 횟수도 지운다. */
    fun onReady() {
        waiting = false
        attempts = 0
    }

    internal companion object {
        const val MAX_ATTEMPTS = 3
    }
}
