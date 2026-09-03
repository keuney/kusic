package com.keuney.music.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keuney.music.core.model.AppError
import com.keuney.music.core.model.AppErrorException
import com.keuney.music.core.model.Track
import com.keuney.music.core.search.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 검색 화면이 그릴 수 있는 상태의 전부다. */
internal sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val tracks: List<Track>) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val error: AppError) : SearchUiState
}

/**
 * 검색만 담당한다. 재생은 PlayerViewModel이 맡으므로 이 클래스는 Android 재생 의존성이 없고
 * 일반 단위 검사로 확인할 수 있다.
 */
@HiltViewModel
internal class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        val trimmed = query.trim()
        // 이전 검색이 남아 늦게 도착한 결과로 화면이 덮이지 않게 한다.
        searchJob?.cancel()
        if (trimmed.isEmpty()) {
            mutableState.value = SearchUiState.Idle
            return
        }
        mutableState.value = SearchUiState.Loading
        searchJob = viewModelScope.launch {
            mutableState.value = repository.search(trimmed).fold(
                onSuccess = { if (it.isEmpty()) SearchUiState.Empty else SearchUiState.Success(it) },
                onFailure = { SearchUiState.Error((it as? AppErrorException)?.error ?: AppError.Unknown) },
            )
        }
    }

    /** 검색어를 지웠을 때처럼 결과를 치우고 처음 상태로 되돌린다. */
    fun clear() {
        searchJob?.cancel()
        mutableState.value = SearchUiState.Idle
    }

    override fun onCleared() {
        searchJob?.cancel()
    }
}
