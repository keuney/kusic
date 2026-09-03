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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.flowOf
import com.keuney.music.R
import com.keuney.music.core.player.ConnectionState
import com.keuney.music.core.player.PlaybackPhase
import com.keuney.music.core.player.RepeatMode
import com.keuney.music.feature.library.AddToPlaylistDialog
import com.keuney.music.feature.library.LibraryViewModel
import com.keuney.music.feature.library.PlaylistNameDialog
import com.keuney.music.ui.components.Artwork
import com.keuney.music.ui.format.formatDuration

/**
 * 전체 화면 플레이어. 미니 플레이어를 눌러 들어오고 뒤로 가기로 떠났던 탭으로 돌아간다.
 *
 * 즐겨찾기는 저장소에서 흐르는 값을 그대로 보여준다. 눌렀다는 사실을 따로 기억하면 저장이
 * 실패했을 때 화면이 거짓을 보인다. 대기열 버튼은 KM-097에서 대기열 화면으로 연결했다.
 */
@Composable
internal fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val wifiOnly by viewModel.wifiOnlyPlayback.collectAsStateWithLifecycle()
    val meteredBlocked by viewModel.meteredPlaybackBlocked.collectAsStateWithLifecycle()
    var draggedPosition by remember { mutableStateOf<Float?>(null) }
    var pendingSeek by remember { mutableStateOf<PendingSeek?>(null) }
    val connected = connection == ConnectionState.Connected
    val nowPlaying = playback.nowPlaying
    val favoriteFlow = remember(nowPlaying?.mediaId) {
        nowPlaying?.let { libraryViewModel.isFavorite(it.mediaId) } ?: flowOf(false)
    }
    val isFavorite by favoriteFlow.collectAsStateWithLifecycle(false)
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    var addingToPlaylist by remember { mutableStateOf(false) }
    var namingNewPlaylist by remember { mutableStateOf(false) }
    // 손가락 → 아직 도달하지 않은 탐색 목표 → 실제 위치 순으로 고른다.
    val shownPositionMs = seekDisplayPositionMs(
        reportedMs = playback.positionMs,
        draggingMs = draggedPosition?.toLong(),
        pending = pendingSeek,
    )
    // 목표에 도달하면 붙잡아 둔 것을 놓는다. 남겨 두면 재생이 진행한 뒤 표시를 다시 가로챈다.
    LaunchedEffect(playback.positionMs) {
        if (pendingSeek?.isSettled(playback.positionMs) == true) pendingSeek = null
    }
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
            value = shownPositionMs.toFloat(),
            onValueChange = { draggedPosition = it },
            onValueChangeFinished = {
                draggedPosition?.let { dragged ->
                    val target = dragged.toLong()
                    // 실제 위치가 목표에 닿을 때까지 표시를 붙잡아 둔다.
                    pendingSeek = PendingSeek(fromMs = playback.positionMs, toMs = target)
                    viewModel.seekTo(target)
                }
                draggedPosition = null
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..playback.durationMs.coerceAtLeast(1).toFloat(),
            enabled = connected && playback.durationMs > 0,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(shownPositionMs), style = MaterialTheme.typography.bodySmall)
            Text(formatDuration(playback.durationMs), style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    nowPlaying?.let {
                        libraryViewModel.setFavorite(it.toTrack(playback.durationMs), !isFavorite)
                    }
                },
                enabled = nowPlaying != null,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.player_favorite_remove else R.string.player_favorite,
                    ),
                )
            }
            // 대기열에 곡이 하나뿐이면 이전은 그 곡의 처음으로, 다음은 명령이 없어 비활성이다.
            OutlinedButton(
                onClick = viewModel::previous,
                enabled = connected && playback.hasPrevious,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.player_previous))
            }
            if (connection == ConnectionState.Unavailable) {
                Button(onClick = viewModel::connect, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.player_retry))
                }
            } else {
                Button(
                    onClick = { if (playback.canPause) viewModel.pause() else viewModel.play() },
                    enabled = connected,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(if (playback.canPause) R.string.player_pause else R.string.player_play))
                }
            }
            OutlinedButton(
                onClick = viewModel::next,
                enabled = connected && playback.hasNext,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.player_next))
            }
            IconButton(onClick = { addingToPlaylist = true }, enabled = nowPlaying != null) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.playlist_add_track))
            }
            IconButton(onClick = onOpenQueue, enabled = connected) {
                Icon(Icons.Filled.List, contentDescription = stringResource(R.string.player_queue))
            }
        }
        // 켜짐·꺼짐이 눈에 보여야 한다. 선택 상태를 가진 칩으로 둔다.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = playback.shuffleEnabled,
                onClick = { viewModel.setShuffleEnabled(!playback.shuffleEnabled) },
                enabled = connected,
                label = { Text(stringResource(R.string.player_shuffle)) },
            )
            // 세 상태를 도는 하나의 칩이다. 지금 무엇인지는 글자가 말한다.
            FilterChip(
                selected = playback.repeatMode != RepeatMode.Off,
                onClick = { viewModel.cycleRepeatMode(playback.repeatMode) },
                enabled = connected,
                label = { Text(stringResource(repeatLabelRes(playback.repeatMode))) },
            )
        }
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
    val currentTrack = nowPlaying?.toTrack(playback.durationMs)
    if (addingToPlaylist && currentTrack != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onSelect = {
                libraryViewModel.addToPlaylist(it.id, currentTrack)
                addingToPlaylist = false
            },
            onCreateNew = {
                addingToPlaylist = false
                namingNewPlaylist = true
            },
            onDismiss = { addingToPlaylist = false },
        )
    }
    if (namingNewPlaylist && currentTrack != null) {
        // 새 재생목록을 만들고 그 곡을 바로 담는다. 만들고 다시 고르게 하면 두 번 묻는 셈이다.
        PlaylistNameDialog(
            titleRes = R.string.playlist_create_title,
            initialName = "",
            onConfirm = {
                libraryViewModel.createPlaylistWith(it, currentTrack)
                namingNewPlaylist = false
            },
            onDismiss = { namingNewPlaylist = false },
        )
    }
}

private fun repeatLabelRes(mode: RepeatMode): Int = when (mode) {
    RepeatMode.Off -> R.string.player_repeat_off
    RepeatMode.All -> R.string.player_repeat_all
    RepeatMode.One -> R.string.player_repeat_one
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
