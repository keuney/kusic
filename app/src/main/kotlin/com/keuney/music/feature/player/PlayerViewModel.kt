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

    override fun onCleared() = connection.disconnect()
}
