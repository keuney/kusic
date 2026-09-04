package com.keuney.music.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keuney.music.R
import com.keuney.music.core.player.NowPlaying
import com.keuney.music.ui.components.Artwork

/**
 * 지금 재생 중인 곡을 한 줄로 보여주고 재생·일시정지를 제공한다. 줄을 누르면 [onClick]으로
 * 전체 화면 플레이어로 간다. 재생·일시정지 버튼은 자기 몫만 처리하고 줄 이동을 일으키지 않는다.
 */
@Composable
internal fun MiniPlayer(
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        // 줄을 누르면 전체 화면으로 간다. 그 뜻을 이름으로 남긴다.
        modifier = modifier.clickable(
            onClickLabel = stringResource(R.string.a11y_open_player),
            onClick = onClick,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 제목이 바로 옆에 있으므로 이미지는 장식이다.
        Artwork(
            model = nowPlaying.artworkUri,
            contentDescription = null,
            modifier = Modifier.size(ARTWORK_SIZE).clip(RoundedCornerShape(4.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nowPlaying.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (nowPlaying.artist.isNotBlank()) {
                Text(
                    text = nowPlaying.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Button(onClick = onPlayPause, enabled = enabled) {
            Text(stringResource(if (isPlaying) R.string.player_pause else R.string.player_play))
        }
    }
}

private val ARTWORK_SIZE = 48.dp
