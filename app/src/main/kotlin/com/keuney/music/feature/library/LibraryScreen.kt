package com.keuney.music.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.keuney.music.core.model.Playlist
import com.keuney.music.core.model.Track
import com.keuney.music.ui.components.TrackRow

/**
 * 라이브러리 화면. 즐겨찾기와 재생목록 두 구획을 보여준다.
 *
 * 두 구획이 한 목록 안에서 함께 흐른다. 각자 스크롤하게 만들면 화면 안에 스크롤이 둘이 되고
 * Compose는 같은 방향으로 겹친 스크롤을 허용하지 않는다.
 *
 * 최근 재생 구획은 KM-115에서 더하고 화면 전체 구성은 KM-116이 다듬는다.
 */
@Composable
internal fun LibraryScreen(
    viewModel: LibraryViewModel,
    selectEnabled: Boolean,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item(key = "favorites-header") {
            SectionHeader(stringResource(R.string.library_favorites))
        }
        if (favorites.isEmpty()) {
            item(key = "favorites-empty") {
                EmptyNote(stringResource(R.string.library_favorites_empty))
            }
        } else {
            itemsIndexed(favorites, key = { _, track -> "favorite-${track.id}" }) { index, track ->
                // 고른 곡부터 재생하고 즐겨찾기 목록 전체가 대기열이 된다. 검색과 같은 방식이다.
                TrackRow(
                    track = track,
                    enabled = selectEnabled,
                    onClick = { onSelect(favorites, index) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item(key = "playlists-header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(stringResource(R.string.library_playlists), Modifier.weight(1f))
                TextButton(onClick = { creating = true }) {
                    Text(stringResource(R.string.playlist_create))
                }
            }
        }
        if (playlists.isEmpty()) {
            item(key = "playlists-empty") {
                EmptyNote(stringResource(R.string.library_playlists_empty))
            }
        } else {
            items(playlists, key = { "playlist-${it.id}" }) { playlist ->
                PlaylistRow(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
            }
        }
    }
    if (creating) {
        PlaylistNameDialog(
            titleRes = R.string.playlist_create_title,
            initialName = "",
            onConfirm = {
                viewModel.createPlaylist(it)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    )
}

/** 이름 바꾸기와 삭제는 재생목록 화면에 있다. 줄마다 버튼을 붙이면 실수로 누르기 쉽다. */
@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.playlist_track_count, playlist.trackCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
