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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.model.Track
import com.keuney.music.ui.components.TrackRow

/**
 * 한 구획의 곡 전체 목록. 요약 화면의 "더 보기"로 들어온다.
 *
 * 즐겨찾기 구획에서는 줄마다 해제 버튼을 둔다. 이 화면에 들어온 것은 그 목록을 정리하려는
 * 뜻이므로 여기서 손볼 수 있어야 한다. 요약 화면에는 두지 않는다. 재생목록 화면과 같은
 * 판단이다(ADR-059).
 *
 * 최근 재생에는 줄마다 버튼이 없다. 한 곡만 빼는 것은 뜻이 약하고, 전체 지우기는 요약 화면
 * 머리에 있다.
 */
@Composable
internal fun LibrarySectionScreen(
    viewModel: LibraryViewModel,
    section: LibrarySection,
    selectEnabled: Boolean,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
    onBack: () -> Unit,
) {
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val tracks = when (section) {
        LibrarySection.Recent -> recent
        LibrarySection.Favorites -> favorites
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.player_back))
            }
            Text(stringResource(section.titleRes), style = MaterialTheme.typography.titleLarge)
        }
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(
                        when (section) {
                            LibrarySection.Recent -> R.string.library_recent_empty
                            LibrarySection.Favorites -> R.string.library_favorites_empty
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
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
                        if (section == LibrarySection.Favorites) {
                            IconButton(onClick = { viewModel.setFavorite(track, favorite = false) }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.player_favorite_remove),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
