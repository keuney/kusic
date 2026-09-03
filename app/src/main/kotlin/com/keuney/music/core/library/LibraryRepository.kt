package com.keuney.music.core.library

import com.keuney.music.core.model.Playlist
import com.keuney.music.core.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * 로컬 라이브러리 경계(ARCHITECTURE 4·6). 화면과 ViewModel은 DAO를 알지 못한다(AGENTS.md 10).
 *
 * 읽기는 모두 Flow다. 즐겨찾기를 켜면 라이브러리 목록이 스스로 바뀌어야 하고, 그것을 화면이
 * 다시 물어보게 만들면 화면마다 갱신 시점을 따로 챙겨야 한다.
 *
 * 쓰기는 [Track]을 받는다. 즐겨찾기·재생목록·재생 기록은 곡 메타데이터가 로컬에 있어야 성립하므로
 * 구현이 곡을 먼저 저장한다. 화면이 그 순서를 신경 쓸 필요가 없다.
 *
 * M7의 세 기능(즐겨찾기·재생목록·재생 기록)을 한 인터페이스에 둔다. ARCHITECTURE 4가
 * LibraryRepositoryImpl 하나를 지정하며, v0.1의 라이브러리는 이 정도 크기다. 언제 무엇을 기록할지
 * 같은 정책은 이 경계가 아니라 부르는 쪽(KM-112~115)이 정한다.
 */
interface LibraryRepository {
    /** 최근에 추가한 즐겨찾기부터. */
    val favorites: Flow<List<Track>>

    fun isFavorite(trackId: String): Flow<Boolean>

    suspend fun setFavorite(track: Track, favorite: Boolean)

    /** 최근에 만든 재생목록부터. 담긴 곡 수를 함께 준다. */
    val playlists: Flow<List<Playlist>>

    /** 담은 자리 순서대로. */
    fun playlistTracks(playlistId: Long): Flow<List<Track>>

    /** 만든 재생목록의 ID. 같은 이름을 막지 않는다. */
    suspend fun createPlaylist(name: String): Long

    suspend fun renamePlaylist(playlistId: Long, name: String)

    suspend fun deletePlaylist(playlistId: Long)

    /** 같은 곡을 한 재생목록에 여러 번 담을 수 있다. 뒤에 붙는다. */
    suspend fun addToPlaylist(playlistId: Long, track: Track)

    /** 그 곡의 항목을 모두 뺀다. */
    suspend fun removeFromPlaylist(playlistId: Long, trackId: String)

    /** 최근에 들은 곡부터, 같은 곡은 한 번만. */
    fun recentlyPlayed(limit: Int): Flow<List<Track>>

    suspend fun recordPlayback(track: Track)

    suspend fun clearPlaybackHistory()
}
