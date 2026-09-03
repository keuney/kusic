package com.keuney.music.feature.library

import com.keuney.music.core.library.LibraryRepository
import com.keuney.music.core.model.Playlist
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** KM-112: 화면이 저장소에서 흐르는 값만 보고, 토글이 저장소까지 닿는지 확인한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeLibrary()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun favoritesStartEmptyAndFollowTheRepository() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)
        advanceUntilIdle()
        assertEquals(emptyList<Track>(), viewModel.favorites.value)

        repository.favoriteTracks.value = listOf(track("a"))
        advanceUntilIdle()

        assertEquals(listOf(track("a")), viewModel.favorites.value)
    }

    @Test
    fun turningTheFavoriteOnReachesTheRepository() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)

        viewModel.setFavorite(track("a"), favorite = true)
        advanceUntilIdle()

        assertEquals(listOf(track("a") to true), repository.calls)
    }

    @Test
    fun turningTheFavoriteOffReachesTheRepository() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)

        viewModel.setFavorite(track("a"), favorite = false)
        advanceUntilIdle()

        assertEquals(listOf(track("a") to false), repository.calls)
    }

    @Test
    fun theFavoriteFlagComesFromTheRepository() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)

        assertFalse(viewModel.isFavorite("a").first())

        repository.favoriteTracks.value = listOf(track("a"))

        assertTrue(viewModel.isFavorite("a").first())
        assertFalse("다른 곡까지 즐겨찾기로 보면 안 된다", viewModel.isFavorite("b").first())
    }

    private fun track(id: String) =
        Track(id, "제목 $id", "아티스트", null, 180_000, SourceType.Remote)

    private class FakeLibrary : LibraryRepository {
        val favoriteTracks = MutableStateFlow(emptyList<Track>())
        val calls = mutableListOf<Pair<Track, Boolean>>()

        override val favorites: Flow<List<Track>> = favoriteTracks

        override fun isFavorite(trackId: String): Flow<Boolean> =
            favoriteTracks.map { tracks -> tracks.any { it.id == trackId } }

        override suspend fun setFavorite(track: Track, favorite: Boolean) {
            calls += track to favorite
        }

        override val playlists: Flow<List<Playlist>> = MutableStateFlow(emptyList())
        override fun playlistTracks(playlistId: Long): Flow<List<Track>> = MutableStateFlow(emptyList())
        override suspend fun createPlaylist(name: String): Long = 0
        override suspend fun renamePlaylist(playlistId: Long, name: String) = Unit
        override suspend fun deletePlaylist(playlistId: Long) = Unit
        override suspend fun addToPlaylist(playlistId: Long, track: Track) = Unit
        override suspend fun removeFromPlaylist(playlistId: Long, trackId: String) = Unit
        override fun recentlyPlayed(limit: Int): Flow<List<Track>> = MutableStateFlow(emptyList())
        override suspend fun recordPlayback(track: Track) = Unit
        override suspend fun clearPlaybackHistory() = Unit
    }
}
