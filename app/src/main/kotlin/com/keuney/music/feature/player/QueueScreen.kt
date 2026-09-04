package com.keuney.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.player.NowPlaying
import com.keuney.music.ui.components.Artwork

/**
 * 대기열 화면. 넣은 순서를 보여주고 현재 곡을 표시하며 빼기와 자리 옮기기를 제공한다.
 *
 * 셔플이 켜졌을 때의 실제 재생 순서는 보여줄 수 없다. 세션이 컨트롤러에 보내는 Timeline에 셔플
 * 순서가 실려 오지 않기 때문이다(ADR-053). 그래서 순서는 늘 넣은 순서이고, 셔플이 켜져 있으면
 * 그 사실을 문구로 알린다.
 *
 * 끌어서 옮기기는 넣지 않았다. Compose에 끌어 정렬하는 기본 목록이 없어 별도 의존성이나 직접
 * 구현이 필요하고 인수 조건의 reorder는 "가능하면"이다. 위·아래 버튼으로 같은 일을 한다.
 */
@Composable
internal fun QueueScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.player_back))
            }
            Text(stringResource(R.string.queue_title), style = MaterialTheme.typography.titleLarge)
        }
        if (playback.shuffleEnabled) {
            Text(
                text = stringResource(R.string.queue_shuffle_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (playback.queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.queue_empty), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(playback.queue, key = { _, track -> track.mediaId }) { index, track ->
                    QueueRow(
                        track = track,
                        current = index == playback.queueIndex,
                        canMoveUp = index > 0,
                        canMoveDown = index < playback.queue.lastIndex,
                        onPlay = { viewModel.seekToQueueItem(index) },
                        onRemove = { viewModel.removeFromQueue(index) },
                        onMoveUp = { viewModel.moveInQueue(index, index - 1) },
                        onMoveDown = { viewModel.moveInQueue(index, index + 1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    track: NowPlaying,
    current: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    // 현재 곡은 배경으로 구분한다. 글자만 굵게 하면 목록에서 눈에 잘 띄지 않는다.
    val background = if (current) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClickLabel = stringResource(R.string.player_play), onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            model = track.artworkUri,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (current) stringResource(R.string.queue_now_playing) else track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = if (current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.queue_move_up))
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.queue_move_down))
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.queue_remove))
        }
    }
}
