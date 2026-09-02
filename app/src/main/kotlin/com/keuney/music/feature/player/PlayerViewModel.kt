package com.keuney.music.feature.player

import androidx.lifecycle.ViewModel
import com.keuney.music.core.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class PlayerViewModel @Inject constructor(
    private val connection: PlayerConnection,
) : ViewModel() {
    val connectionState = connection.state
    val playbackState = connection.playback

    fun connect() = connection.connect()
    fun disconnect() = connection.disconnect()
    fun play() = connection.play()
    fun pause() = connection.pause()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)

    /** KM-057 확인용 고정 Track. KM-058에서 검색 결과 선택으로 대체한다. */
    fun playSampleTrack() = connection.playTrack(SAMPLE_TRACK_ID, SAMPLE_TRACK_TITLE, SAMPLE_TRACK_ARTIST)

    override fun onCleared() = connection.disconnect()

    private companion object {
        const val SAMPLE_TRACK_ID = "gdZLi9oWNZg"
        const val SAMPLE_TRACK_TITLE = "원격 스트림 확인용 트랙"
        const val SAMPLE_TRACK_ARTIST = "Keuney Music"
    }
}
