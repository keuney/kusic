package com.keuney.music.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keuney.music.core.model.Track
import com.keuney.music.ui.components.TrackRow

/**
 * 검색 결과 곡 목록. 항목을 고르면 목록 전체와 그 자리를 [onSelect]로 넘긴다. 받는 쪽이 대기열을
 * 만들 수 있어야 하므로 고른 곡만 넘기지 않는다. 재생 여부는 이 목록이 판단하지 않는다.
 */
@Composable
internal fun SearchResultList(
    tracks: List<Track>,
    selectEnabled: Boolean,
    onSelect: (tracks: List<Track>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                track = track,
                enabled = selectEnabled,
                onClick = { onSelect(tracks, index) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
