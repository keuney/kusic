package com.keuney.music.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.model.Playlist
import com.keuney.music.core.model.Track
import com.keuney.music.feature.library.LibraryViewModel
import com.keuney.music.ui.components.Artwork

/**
 * 홈 화면. PRD 35의 세 구획(최근 재생·즐겨찾기·재생목록)을 가진다.
 *
 * 라이브러리 탭과 같은 세 구획을 쓰지만 하는 일이 다르다. **홈은 한 번 눌러 다시 듣는 자리**이고
 * 라이브러리는 전부 보고 정리하는 자리다. 그래서 홈은 가로로 흐르는 카드를 쓰고 지우기·해제 같은
 * 정리 버튼을 두지 않는다. 라이브러리는 세로 목록과 정리 버튼을 가진다.
 *
 * 같은 세 구획을 두 탭에서 똑같은 모양으로 보여주면 탭 하나가 남는다. PRD가 정한 것은 홈에 세
 * 구획이 있어야 한다는 것이고 어떤 모양인지는 정하지 않았다.
 *
 * 카드를 누르면 그 줄 전체가 대기열이 되고 누른 곡부터 재생한다. 검색 결과·라이브러리와 같은
 * 방식이다.
 */
@Composable
internal fun HomeScreen(
    viewModel: LibraryViewModel,
    selectEnabled: Boolean,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onGoSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    if (recent.isEmpty() && favorites.isEmpty() && playlists.isEmpty()) {
        EmptyHome(onGoSearch = onGoSearch, modifier = modifier)
        return
    }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 비어 있는 구획은 그리지 않는다. 홈에서 "없습니다"를 읽을 이유가 없다. 무엇이 없는지는
        // 라이브러리가 말해 준다.
        trackRow(
            titleRes = R.string.library_recent,
            tracks = recent,
            selectEnabled = selectEnabled,
            keyPrefix = "recent",
            onSelect = onSelect,
        )
        trackRow(
            titleRes = R.string.library_favorites,
            tracks = favorites,
            selectEnabled = selectEnabled,
            keyPrefix = "favorite",
            onSelect = onSelect,
        )
        if (playlists.isNotEmpty()) {
            item(key = "playlists-header") { SectionTitle(R.string.library_playlists) }
            item(key = "playlists-row") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(playlists, key = Playlist::id) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }
    }
}

/** 머리와 가로 카드 줄이 한 구획이다. 두 곡 구획이 같은 모양을 쓴다. */
private fun LazyListScope.trackRow(
    titleRes: Int,
    tracks: List<Track>,
    selectEnabled: Boolean,
    keyPrefix: String,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
) {
    if (tracks.isEmpty()) return
    item(key = "$keyPrefix-header") { SectionTitle(titleRes) }
    item(key = "$keyPrefix-row") {
        // 가로 줄은 보이는 것만 만든다. 개수를 잘라 낼 이유가 없고, 자르면 누른 자리와 대기열의
        // 자리가 어긋날 위험만 생긴다.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(tracks, key = { _, track -> "$keyPrefix-${track.id}" }) { index, track ->
                TrackCard(track = track, enabled = selectEnabled, onClick = { onSelect(tracks, index) })
            }
        }
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
    )
}

/** 홈의 곡 카드. 이미지가 크고 글자는 아래 두 줄까지다. */
@Composable
private fun TrackCard(track: Track, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(CARD_WIDTH)
            .clickable(enabled = enabled, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // 제목이 바로 아래 있으므로 이미지는 장식이다.
        Artwork(
            model = track.artworkUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
        )
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (track.artist.isNotBlank()) {
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 재생목록 카드. 이미지가 없으므로 곡 카드와 같은 크기의 빈 면에 곡 수를 얹는다.
 *
 * 누르면 재생하지 않고 재생목록 화면을 연다. 재생목록은 곡이 아니라 곡의 묶음이고, 무엇이
 * 담겼는지 보고 고르는 것이 먼저다.
 */
@Composable
private fun PlaylistCard(playlist: Playlist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(CARD_WIDTH)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(CARD_WIDTH)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.playlist_track_count, playlist.trackCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 아무것도 없을 때. 홈에서 할 수 있는 일은 들은 것을 다시 듣는 것이므로, 들은 것이 없으면
 * 검색으로 보낸다. 라이브러리의 빈 화면이 재생목록 만들기를 권하는 것과 같은 판단이다.
 */
@Composable
private fun EmptyHome(onGoSearch: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onGoSearch) { Text(stringResource(R.string.home_go_search)) }
        }
    }
}

private val CARD_WIDTH = 140.dp
