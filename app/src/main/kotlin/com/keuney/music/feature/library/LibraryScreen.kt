package com.keuney.music.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
 * 라이브러리 화면. 지금은 즐겨찾기만 보여준다.
 *
 * 재생목록과 최근 재생 구획은 각 기능 작업(KM-113·115)에서 더하고, 화면 전체 구성은 KM-116이
 * 다듬는다. 즐겨찾기 목록을 여기 두는 것은 KM-112의 인수 조건(Library display)이다.
 */
@Composable
internal fun LibraryScreen(
    viewModel: LibraryViewModel,
    selectEnabled: Boolean,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.library_favorites),
            style = MaterialTheme.typography.titleMedium,
        )
        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.library_favorites_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(favorites, key = { _, track -> track.id }) { index, track ->
                    // 고른 곡부터 재생하고 즐겨찾기 목록 전체가 대기열이 된다. 검색과 같은 방식이다.
                    TrackRow(
                        track = track,
                        enabled = selectEnabled,
                        onClick = { onSelect(favorites, index) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
