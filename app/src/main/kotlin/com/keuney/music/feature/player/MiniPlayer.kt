package com.keuney.music.feature.player

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
 * 지금 재생 중인 곡을 한 줄로 보여주고 재생·일시정지를 제공한다.
 *
 * 눌러서 Now Playing으로 가는 동작은 아직 없다. 화면 전환이 필요하고 내비게이션은 KM-150,
 * 목적 화면은 KM-092다. KM-072와 같은 이유로 백로그 순서를 지킨다.
 */
@Composable
internal fun MiniPlayer(
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
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
