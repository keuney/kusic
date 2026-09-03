package com.keuney.music.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keuney.music.core.model.Track
import com.keuney.music.core.player.NetworkPolicy
import com.keuney.music.core.player.PlayerConnection
import com.keuney.music.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 재생과 재생 관련 설정만 담당한다. 검색은 SearchViewModel이 맡는다. */
@HiltViewModel
internal class PlayerViewModel @Inject constructor(
    private val connection: PlayerConnection,
    private val settings: SettingsRepository,
    private val networkPolicy: NetworkPolicy,
) : ViewModel() {
    val connectionState = connection.state
    val playbackState = connection.playback

    val wifiOnlyPlayback: StateFlow<Boolean> = settings.wifiOnlyPlayback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 설정이 켜져 있고 지금 연결이 측정 요금제면 새로 내려받는 재생이 막힌다. */
    val meteredPlaybackBlocked: StateFlow<Boolean> = combine(
        settings.wifiOnlyPlayback,
        connection.playback,
    ) { wifiOnly, _ -> wifiOnly && networkPolicy.isMetered() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setWifiOnlyPlayback(enabled: Boolean) {
        viewModelScope.launch { settings.setWifiOnlyPlayback(enabled) }
    }

    fun connect() = connection.connect()
    fun disconnect() = connection.disconnect()
    fun play() = connection.play()
    fun pause() = connection.pause()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)
    fun previous() = connection.seekToPrevious()
    fun next() = connection.seekToNext()
    fun setShuffleEnabled(enabled: Boolean) = connection.setShuffleEnabled(enabled)

    /** 대기열에는 Track ID와 표시용 metadata만 전달한다. 스트림 주소는 서비스가 해석한다. */
    fun playTrack(track: Track) =
        connection.playTrack(track.id, track.title, track.artist, track.artworkUrl)

    override fun onCleared() = connection.disconnect()
}
