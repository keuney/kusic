package com.keuney.music.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.model.Track
import com.keuney.music.ui.components.TrackRow

/**
 * 한 재생목록의 곡 목록. 이름 바꾸기·삭제·곡 빼기를 여기서 한다.
 *
 * 재생목록 목록의 줄에는 조작 버튼을 두지 않았다. 그 목록에서 실수로 삭제를 누르면 되돌릴 수
 * 없다. 이 화면에 들어온 것은 그 재생목록을 다루려는 뜻이므로 여기 둔다. 대기열 화면과 같은
 * 판단이다.
 *
 * 곡을 누르면 그 곡부터 재생하고 재생목록 전체가 대기열이 된다. 이어 듣기와 이전·다음의 인수
 * 조건 확인은 KM-114다.
 */
@Composable
internal fun PlaylistScreen(
    viewModel: LibraryViewModel,
    playlistId: Long,
    selectEnabled: Boolean,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
    onBack: () -> Unit,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    val tracks by remember(playlistId) { viewModel.playlistTracks(playlistId) }
        .collectAsStateWithLifecycle(emptyList())
    var renaming by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.player_back))
            }
            Text(
                text = playlist?.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { renaming = true }, enabled = playlist != null) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.playlist_rename))
            }
            IconButton(
                onClick = {
                    viewModel.deletePlaylist(playlistId)
                    onBack()
                },
                enabled = playlist != null,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.playlist_delete))
            }
        }
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.playlist_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(tracks, key = { index, track -> "$index-${track.id}" }) { index, track ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrackRow(
                            track = track,
                            enabled = selectEnabled,
                            onClick = { onSelect(tracks, index) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.removeFromPlaylist(playlistId, track.id) }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.playlist_remove_track),
                            )
                        }
                    }
                }
            }
        }
    }
    if (renaming && playlist != null) {
        PlaylistNameDialog(
            titleRes = R.string.playlist_rename_title,
            initialName = playlist.name,
            onConfirm = {
                viewModel.renamePlaylist(playlistId, it)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
}
