package com.keuney.music.core.player

import androidx.media3.common.PlaybackException

/**
 * 재생이 멈춘 이유 중 화면과 회복 판단에 필요한 만큼만 남긴 분류.
 *
 * 화면은 Media3 오류 코드를 직접 해석하지 않는다(ARCHITECTURE 19). 원인 문자열도 오지 않는다.
 * 사용자에게 필요한 것은 "기다리면 되는가"이고, 그 답이 이 두 가지다.
 */
internal enum class PlaybackFailure {
    /** 연결이 끊겼거나 응답이 없다. 연결이 돌아오면 그대로 이어질 수 있다. */
    Network,

    /** 곡을 가져올 수 없다. 기다린다고 달라지지 않으므로 다른 곡을 골라야 한다. */
    Source,
}

/**
 * 오류 코드를 분류로 바꾼다. 오류가 없으면 null이다.
 *
 * 연결 실패와 시간 초과만 네트워크로 본다. 분류하지 못한 입출력 오류(ERROR_CODE_IO_UNSPECIFIED)는
 * 네트워크로 보지 않는다. 그 코드는 "무엇인지 모르겠다"는 뜻이고, 연결이 돌아올 때마다 같은
 * 실패를 되풀이할 뿐이다. 진짜 네트워크 실패는 HTTP 계층이 연결 실패 코드로 알려 주고, 주소
 * 해석 실패도 [TrackStreamResolver]가 같은 코드로 바꿔 보낸다.
 */
internal fun playbackFailureOf(errorCode: Int?): PlaybackFailure? = when (errorCode) {
    null -> null
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    -> PlaybackFailure.Network
    else -> PlaybackFailure.Source
}
