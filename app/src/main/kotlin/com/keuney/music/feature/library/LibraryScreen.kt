package com.keuney.music.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.model.Playlist
import com.keuney.music.core.model.Track
import com.keuney.music.ui.components.TrackRow

/**
 * 라이브러리 요약 화면. 최근 재생·즐겨찾기·재생목록 세 구획을 보여준다. 순서는 PRD 35를 따른다.
 *
 * 곡 구획은 앞의 몇 개만 보여주고 나머지는 "더 보기"로 넘긴다. 전부 쏟으면 아래 구획이 화면
 * 밖으로 밀려나 재생목록에 닿으려면 수십 곡을 지나야 한다.
 *
 * 여기서는 곡을 지우거나 빼지 않는다. 줄마다 버튼을 두면 훑어보다 실수로 누르기 쉽다. 정리는
 * 각 구획의 전체 목록 화면에서 한다. 재생목록도 같은 규칙이다(ADR-059).
 *
 * 세 구획이 모두 비면 구획을 나열하지 않는다. 첫 실행에서 "없습니다"가 세 줄인 화면은 무엇을
 * 해야 하는지 알려주지 않는다.
 */
@Composable
internal fun LibraryScreen(
    viewModel: LibraryViewModel,
    selectEnabled: Boolean,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSection: (LibrarySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    if (recent.isEmpty() && favorites.isEmpty() && playlists.isEmpty()) {
        EmptyLibrary(onCreatePlaylist = { creating = true }, modifier = modifier)
    } else {
        LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            trackSection(
                titleRes = R.string.library_recent,
                tracks = recent,
                selectEnabled = selectEnabled,
                keyPrefix = "recent",
                topPadding = 0.dp,
                emptyRes = R.string.library_recent_empty,
                action = {
                    if (recent.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearPlaybackHistory) {
                            Text(stringResource(R.string.library_recent_clear))
                        }
                    }
                },
                onSelect = onSelect,
                onMore = { onOpenSection(LibrarySection.Recent) },
            )
            trackSection(
                titleRes = R.string.library_favorites,
                tracks = favorites,
                selectEnabled = selectEnabled,
                keyPrefix = "favorite",
                topPadding = 16.dp,
                emptyRes = R.string.library_favorites_empty,
                action = {},
                onSelect = onSelect,
                onMore = { onOpenSection(LibrarySection.Favorites) },
            )
            item(key = "playlists-header") {
                SectionRow(titleRes = R.string.library_playlists, topPadding = 16.dp) {
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
                // 재생목록은 줄이 짧고 개수가 적어 전부 보여도 아래를 밀어내지 않는다.
                items(playlists, key = { "playlist-${it.id}" }) { playlist ->
                    PlaylistRow(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                }
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

/** 머리·앞부분 몇 곡·더 보기까지가 한 구획이다. 두 곡 구획이 같은 모양을 쓴다. */
private fun LazyListScope.trackSection(
    titleRes: Int,
    tracks: List<Track>,
    selectEnabled: Boolean,
    keyPrefix: String,
    topPadding: Dp,
    emptyRes: Int,
    action: @Composable () -> Unit,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
    onMore: () -> Unit,
) {
    item(key = "$keyPrefix-header") {
        SectionRow(titleRes = titleRes, topPadding = topPadding, action = action)
    }
    if (tracks.isEmpty()) {
        item(key = "$keyPrefix-empty") { EmptyNote(stringResource(emptyRes)) }
        return
    }
    val preview = tracks.take(LIBRARY_SECTION_PREVIEW)
    itemsIndexed(preview, key = { _, track -> "$keyPrefix-${track.id}" }) { index, track ->
        // 고른 곡부터 재생하고 구획의 곡 전체가 대기열이 된다. 검색과 같은 방식이다.
        TrackRow(
            track = track,
            enabled = selectEnabled,
            onClick = { onSelect(tracks, index) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (tracks.size > preview.size) {
        item(key = "$keyPrefix-more") {
            TextButton(onClick = onMore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.library_more))
            }
        }
    }
}

@Composable
private fun SectionRow(titleRes: Int, topPadding: Dp, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = topPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        action()
    }
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
            .clickable(onClickLabel = stringResource(R.string.a11y_open), onClick = onClick)
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

/** 아무것도 없을 때. 무엇을 하면 채워지는지 알려주고 재생목록은 여기서도 만들 수 있게 둔다. */
@Composable
private fun EmptyLibrary(onCreatePlaylist: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onCreatePlaylist) {
                Text(stringResource(R.string.playlist_create_title))
            }
        }
    }
}
