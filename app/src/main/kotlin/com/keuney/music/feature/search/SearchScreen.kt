package com.keuney.music.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keuney.music.R
import com.keuney.music.core.model.AppError
import com.keuney.music.core.model.Track

/**
 * 검색어 입력과 검색 결과만 담당한다. 재생 제어는 이 화면 밖에 있고, 결과를 고른 뒤 무엇을 할지는
 * [onSelect]로 넘긴다. 화면 전환이 붙기 전이라 지금은 플레이어 화면 안에 배치된다(KM-150).
 */
@Composable
internal fun SearchScreen(
    viewModel: SearchViewModel,
    selectEnabled: Boolean,
    onSelect: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                // 검색어를 지웠으면 이전 결과도 함께 치운다.
                if (it.isBlank()) viewModel.clear()
            },
            label = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { viewModel.search(query) }, enabled = query.isNotBlank()) {
            Text(stringResource(R.string.search_action))
        }
        SearchResults(
            state = state,
            selectEnabled = selectEnabled,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

/** [SearchUiState]의 다섯 상태를 모두 그린다. 어느 상태에서도 화면이 비어 보이지 않게 한다. */
@Composable
private fun SearchResults(
    state: SearchUiState,
    selectEnabled: Boolean,
    onSelect: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        SearchUiState.Idle -> Column(modifier) {}
        SearchUiState.Loading -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(stringResource(R.string.search_searching))
        }
        SearchUiState.Empty -> Text(stringResource(R.string.search_empty), modifier = modifier)
        is SearchUiState.Error -> Text(
            text = stringResource(state.error.messageRes()),
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
        is SearchUiState.Success -> SearchResultList(
            tracks = state.tracks,
            selectEnabled = selectEnabled,
            onSelect = onSelect,
            modifier = modifier,
        )
    }
}

/** 사용자에게 보여줄 문구. 원문 예외나 응답 내용은 여기까지 오지 않는다. */
private fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    AppError.SourceUnavailable -> R.string.error_source_unavailable
    AppError.PlaybackUnavailable -> R.string.error_playback_unavailable
    AppError.GeoRestricted -> R.string.error_geo_restricted
    AppError.Unknown -> R.string.error_unknown
}
