package com.keuney.music.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.player.ConnectionState
import com.keuney.music.core.player.PlaybackPhase
import com.keuney.music.ui.components.Artwork
import com.keuney.music.ui.format.formatDuration

/**
 * 전체 화면 플레이어. 미니 플레이어를 눌러 들어오고 뒤로 가기로 떠났던 탭으로 돌아간다.
 *
 * 즐겨찾기와 대기열 버튼은 자리만 있고 눌리지 않는다. 즐겨찾기 저장은 KM-112, 대기열 화면은
 * KM-097 소속이라 여기서 만들지 않는다. 사용자와 합의한 범위다.
 * 이전·다음은 KM-094, 끌어서 탐색의 인수 조건은 KM-093이다.
 */
@Composable
internal fun NowPlayingScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val wifiOnly by viewModel.wifiOnlyPlayback.collectAsStateWithLifecycle()
    val meteredBlocked by viewModel.meteredPlaybackBlocked.collectAsStateWithLifecycle()
    var draggedPosition by remember { mutableStateOf<Float?>(null) }
    val connected = connection == ConnectionState.Connected
    val nowPlaying = playback.nowPlaying
    Column(
        // 제목이 두 줄이거나 요금제 안내가 붙으면 세로가 모자랄 수 있다. 잘리지 않게 흘린다.
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.player_back))
            }
        }
        Artwork(
            model = nowPlaying?.artworkUri,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
        )
        Text(
            text = nowPlaying?.title.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = nowPlaying?.artist.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(stringResource(statusRes(connection, playback.phase)))
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(draggedPosition?.toLong() ?: playback.positionMs), style = MaterialTheme.typography.bodySmall)
            Text(formatDuration(playback.durationMs), style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 즐겨찾기 저장은 KM-112가 만든다. 그때까지 눌리지 않는다.
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.FavoriteBorder, contentDescription = stringResource(R.string.player_favorite))
            }
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
            // 대기열 화면은 KM-097이 만든다. 그때까지 눌리지 않는다.
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.List, contentDescription = stringResource(R.string.player_queue))
            }
        }
        Text(
            text = stringResource(R.string.player_not_ready_action),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // WiFi 전용 설정(KM-137)은 설정 화면(KM-153)이 생기면 그쪽으로 옮긴다.
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

private fun statusRes(connection: ConnectionState, phase: PlaybackPhase): Int = when (connection) {
    ConnectionState.Connecting -> R.string.player_connecting
    ConnectionState.Disconnected -> R.string.player_disconnected
    ConnectionState.Unavailable -> R.string.player_unavailable
    ConnectionState.Connected -> when (phase) {
        PlaybackPhase.Idle -> R.string.player_idle
        PlaybackPhase.Buffering -> R.string.player_buffering
        PlaybackPhase.Playing -> R.string.player_playing
        PlaybackPhase.Paused -> R.string.player_paused
        PlaybackPhase.Ended -> R.string.player_ended
        PlaybackPhase.Unavailable -> R.string.player_unavailable
    }
}
