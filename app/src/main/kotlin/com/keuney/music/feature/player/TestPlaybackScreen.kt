package com.keuney.music.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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

@Composable
internal fun TestPlaybackScreen(viewModel: PlayerViewModel) {
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    var draggedPosition by remember { mutableStateOf<Float?>(null) }
    val connected = connection == ConnectionState.Connected
    val pauseAction = playback.playWhenReady &&
        playback.phase != PlaybackPhase.Ended && playback.phase != PlaybackPhase.Unavailable
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
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.test_audio), style = MaterialTheme.typography.titleMedium)
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
        Text("${formatTime(playback.positionMs)} / ${formatTime(playback.durationMs)}")
        if (connection == ConnectionState.Unavailable) {
            Button(onClick = viewModel::connect) { Text(stringResource(R.string.player_retry)) }
        } else {
            Button(
                onClick = { if (pauseAction) viewModel.pause() else viewModel.play() },
                enabled = connected,
            ) {
                Text(stringResource(if (pauseAction) R.string.player_pause else R.string.player_play))
            }
        }
        Button(onClick = viewModel::playSampleTrack, enabled = connected) {
            Text(stringResource(R.string.player_play_remote))
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
