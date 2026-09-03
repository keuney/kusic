package com.keuney.music.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.player.ConnectionState
import com.keuney.music.core.player.PlaybackPhase
import com.keuney.music.ui.format.formatDuration

/**
 * 전체 화면 재생 확인 화면. 실제 Now Playing 화면은 KM-092가 대신한다.
 *
 * 검색은 KM-150에서 자기 탭으로 옮겼으므로 여기에는 재생만 남는다. 미니 플레이어는 하단
 * 내비게이션 위에 있고 이 화면에서는 접히므로 재생·일시정지 버튼을 여기에 둔다.
 */
@Composable
internal fun TestPlaybackScreen(viewModel: PlayerViewModel) {
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val wifiOnly by viewModel.wifiOnlyPlayback.collectAsStateWithLifecycle()
    val meteredBlocked by viewModel.meteredPlaybackBlocked.collectAsStateWithLifecycle()
    var draggedPosition by remember { mutableStateOf<Float?>(null) }
    val connected = connection == ConnectionState.Connected
    val status = when (connection) {
        ConnectionState.Connecting -> R.string.player_connecting
        ConnectionState.Disconnected -> R.string.player_disconnected
        ConnectionState.Unavailable -> R.string.player_unavailable
        ConnectionState.Connected -> when (playback.phase) {
            PlaybackPhase.Idle -> R.string.player_idle
            PlaybackPhase.Buffering -> R.string.player_buffering
            PlaybackPhase.Playing -> R.string.player_playing
            PlaybackPhase.Paused -> R.string.player_paused
            PlaybackPhase.Ended -> R.string.player_ended
            PlaybackPhase.Unavailable -> R.string.player_unavailable
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.destination_now_playing), style = MaterialTheme.typography.headlineSmall)
        playback.nowPlaying?.let { nowPlaying ->
            Text(nowPlaying.title, style = MaterialTheme.typography.titleMedium)
            if (nowPlaying.artist.isNotBlank()) {
                Text(nowPlaying.artist, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(stringResource(status))
        Slider(
            value = draggedPosition ?: playback.positionMs.toFloat(),
            onValueChange = { draggedPosition = it },
            onValueChangeFinished = {
                draggedPosition?.let { viewModel.seekTo(it.toLong()) }
                draggedPosition = null
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..playback.durationMs.coerceAtLeast(1).toFloat(),
            enabled = connected && playback.durationMs > 0,
        )
        Text("${formatDuration(playback.positionMs)} / ${formatDuration(playback.durationMs)}")
        if (connection == ConnectionState.Unavailable) {
            Button(onClick = viewModel::connect) { Text(stringResource(R.string.player_retry)) }
        } else {
            Button(
                onClick = { if (playback.canPause) viewModel.pause() else viewModel.play() },
                enabled = connected,
            ) {
                Text(stringResource(if (playback.canPause) R.string.player_pause else R.string.player_play))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.wifi_only_label), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = wifiOnly, onCheckedChange = viewModel::setWifiOnlyPlayback)
        }
        if (meteredBlocked) {
            Text(
                stringResource(R.string.wifi_only_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
