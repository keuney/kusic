package com.keuney.music.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keuney.music.core.model.Track
import com.keuney.music.core.player.NetworkPolicy
import com.keuney.music.core.player.PlayerConnection
import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.data.source.MusicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Results(val tracks: List<Track>) : SearchUiState
    data object Empty : SearchUiState
    data object Failed : SearchUiState
}

/**
 * KM-058 수직 슬라이스. 검색은 MusicSource를 직접 호출하며 KM-070의 SearchRepository와
 * KM-071의 SearchViewModel이 이 자리를 대체한다.
 */
@HiltViewModel
internal class PlayerViewModel @Inject constructor(
    private val connection: PlayerConnection,
    private val source: MusicSource,
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

    private val mutableSearch = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = mutableSearch.asStateFlow()
    private var searchJob: Job? = null

    fun connect() = connection.connect()
    fun disconnect() = connection.disconnect()
    fun play() = connection.play()
    fun pause() = connection.pause()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)

    fun search(query: String) {
        val trimmed = query.trim()
        searchJob?.cancel()
        if (trimmed.isEmpty()) {
            mutableSearch.value = SearchUiState.Idle
            return
        }
        mutableSearch.value = SearchUiState.Searching
        searchJob = viewModelScope.launch {
            val tracks = source.search(trimmed)
            mutableSearch.value = tracks.fold(
                onSuccess = { if (it.isEmpty()) SearchUiState.Empty else SearchUiState.Results(it) },
                onFailure = { SearchUiState.Failed },
            )
        }
    }

    /** 대기열에는 Track ID와 metadata만 전달한다. 스트림 주소는 서비스가 해석한다. */
    fun playTrack(track: Track) = connection.playTrack(track.id, track.title, track.artist)

    override fun onCleared() {
        searchJob?.cancel()
        connection.disconnect()
    }
}
