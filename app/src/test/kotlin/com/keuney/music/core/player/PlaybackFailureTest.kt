package com.keuney.music.core.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * KM-132 인수 조건 중 "명확한 실패"를 받치는 규칙이다. 기다리면 되는 실패와 그렇지 않은 실패를
 * 가른다. 이 분류가 화면 문구와 자동 회복 여부를 함께 정한다.
 */
class PlaybackFailureTest {
    @Test
    fun connectionFailuresAreTreatedAsNetwork() {
        listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        ).forEach { code ->
            assertEquals("코드 $code", PlaybackFailure.Network, playbackFailureOf(code))
        }
    }

    @Test
    fun otherFailuresAreTreatedAsSource() {
        listOf(
            // 무엇인지 모르는 입출력 오류는 네트워크로 보지 않는다. 연결이 돌아올 때마다 같은
            // 실패를 되풀이할 뿐이다. 진짜 네트워크 실패에는 전용 코드가 온다.
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            // 응답 상태를 받았다는 것은 연결이 되었다는 뜻이다. 곡 쪽 문제다.
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_UNSPECIFIED,
        ).forEach { code ->
            assertEquals("코드 $code", PlaybackFailure.Source, playbackFailureOf(code))
        }
    }

    @Test
    fun noErrorIsNoFailure() {
        assertNull(playbackFailureOf(null))
    }

    @Test
    fun theFailureReachesTheState() {
        val state = mapPlaybackState(Player.STATE_IDLE, false, true, 0, 0, PlaybackFailure.Network)
        assertEquals(PlaybackFailure.Network, state.failure)
        assertEquals(PlaybackPhase.Unavailable, state.phase)
        assertNull(mapPlaybackState(Player.STATE_READY, true, true, 0, 1000, null).failure)
    }
}
