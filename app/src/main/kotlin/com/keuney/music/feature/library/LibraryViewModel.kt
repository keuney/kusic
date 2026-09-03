package com.keuney.music.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keuney.music.core.library.LibraryRepository
import com.keuney.music.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 라이브러리 화면과 즐겨찾기 버튼이 함께 쓴다. 저장소만 알고 DAO는 알지 못한다(AGENTS.md 10).
 *
 * 즐겨찾기 여부는 앱이 기억하지 않고 저장소에서 흐르는 값을 그대로 쓴다. 눌렀다는 사실을 따로
 * 들고 있으면 저장이 실패했을 때 화면이 거짓을 보인다. 셔플·반복에서도 같은 방식이다.
 */
@HiltViewModel
internal class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {
    /**
     * 즐겨찾기 목록. 최근에 추가한 것부터 온다.
     *
     * 구독이 붙기를 기다리지 않고 바로 읽는다. 상류는 값이 바뀔 때만 흐르는 데이터베이스 관찰이고,
     * 라이브러리 탭이 열리는 순간 이미 목록이 있어야 한다.
     */
    val favorites: StateFlow<List<Track>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 곡 하나의 즐겨찾기 여부. 전체 목록을 받아 뒤지지 않고 필요한 것만 묻는다. */
    fun isFavorite(trackId: String): Flow<Boolean> = repository.isFavorite(trackId)

    fun setFavorite(track: Track, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(track, favorite) }
    }
}
