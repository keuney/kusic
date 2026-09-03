package com.keuney.music.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keuney.music.core.model.Track
import com.keuney.music.ui.components.Artwork
import com.keuney.music.ui.format.formatDuration

/** 검색 결과 곡 목록. 항목을 고르면 [onSelect]로 넘긴다. 재생 여부는 이 목록이 판단하지 않는다. */
@Composable
internal fun SearchResultList(
    tracks: List<Track>,
    selectEnabled: Boolean,
    onSelect: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(tracks, key = Track::id) { track ->
            SearchResultItem(
                track = track,
                selectEnabled = selectEnabled,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    track: Track,
    selectEnabled: Boolean,
    onSelect: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(enabled = selectEnabled) { onSelect(track) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 제목이 바로 옆에 있으므로 이미지는 장식이다. 읽어 주면 같은 내용이 두 번 나온다.
        Artwork(
            model = track.artworkUrl,
            contentDescription = null,
            modifier = Modifier.size(ARTWORK_SIZE).clip(RoundedCornerShape(4.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = trackSubtitle(track)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 제목 아래 한 줄. 길이는 아는 경우에만 붙인다. 공급자가 아티스트나 길이를 주지 않는 결과가 있어
 * 둘 다 없으면 빈 문자열이며 그때는 줄 자체를 그리지 않는다.
 */
internal fun trackSubtitle(track: Track): String = listOfNotNull(
    track.artist.takeIf(String::isNotBlank),
    track.durationMs?.let(::formatDuration),
).joinToString(SUBTITLE_SEPARATOR)

private const val SUBTITLE_SEPARATOR = " · "
private val ARTWORK_SIZE = 56.dp
