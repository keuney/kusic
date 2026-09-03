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

    @Test
    fun playlistsFollowTheRepository() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)
        advanceUntilIdle()
        assertEquals(emptyList<Playlist>(), viewModel.playlists.value)

        repository.playlistRows.value = listOf(Playlist(1, "출근길", 2))
        advanceUntilIdle()

        assertEquals(listOf(Playlist(1, "출근길", 2)), viewModel.playlists.value)
    }

    @Test
    fun createTrimsTheNameAndIgnoresBlank() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)

        viewModel.createPlaylist("  출근길  ")
        viewModel.createPlaylist("   ")
        viewModel.createPlaylist("")
        advanceUntilIdle()

        // 이름 없는 재생목록이 생기면 지울 수밖에 없다.
        assertEquals(listOf("출근길"), repository.created)
    }

    @Test
    fun creatingWithATrackAddsItRightAway() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)

        viewModel.createPlaylistWith("출근길", track("a"))
        advanceUntilIdle()

        assertEquals(listOf("출근길"), repository.created)
        assertEquals(listOf(1L to "a"), repository.added.map { it.first to it.second.id })
    }

    @Test
    fun creatingWithATrackIgnoresABlankName() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)

        viewModel.createPlaylistWith("   ", track("a"))
        advanceUntilIdle()

        assertTrue(repository.created.isEmpty())
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun renameTrimsTheNameAndIgnoresBlank() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)

        viewModel.renamePlaylist(1, "  퇴근길  ")
        viewModel.renamePlaylist(1, "  ")
        advanceUntilIdle()

        assertEquals(listOf(1L to "퇴근길"), repository.renamed)
    }

    @Test
    fun addRemoveAndDeleteReachTheRepository() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(repository)

        viewModel.addToPlaylist(1, track("a"))
        viewModel.removeFromPlaylist(1, "a")
        viewModel.deletePlaylist(1)
        advanceUntilIdle()

        assertEquals(listOf(1L to "a"), repository.added.map { it.first to it.second.id })
        assertEquals(listOf(1L to "a"), repository.removed)
        assertEquals(listOf(1L), repository.deleted)
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

        val playlistRows = MutableStateFlow(emptyList<Playlist>())
        val created = mutableListOf<String>()
        val renamed = mutableListOf<Pair<Long, String>>()
        val deleted = mutableListOf<Long>()
        val added = mutableListOf<Pair<Long, Track>>()
        val removed = mutableListOf<Pair<Long, String>>()

        override val playlists: Flow<List<Playlist>> = playlistRows

        override fun playlistTracks(playlistId: Long): Flow<List<Track>> = MutableStateFlow(emptyList())

        override suspend fun createPlaylist(name: String): Long {
            created += name
            return created.size.toLong()
        }

        override suspend fun renamePlaylist(playlistId: Long, name: String) {
            renamed += playlistId to name
        }

        override suspend fun deletePlaylist(playlistId: Long) {
            deleted += playlistId
        }

        override suspend fun addToPlaylist(playlistId: Long, track: Track) {
            added += playlistId to track
        }

        override suspend fun removeFromPlaylist(playlistId: Long, trackId: String) {
            removed += playlistId to trackId
        }
        override fun recentlyPlayed(limit: Int): Flow<List<Track>> = MutableStateFlow(emptyList())
        override suspend fun recordPlayback(track: Track) = Unit
        override suspend fun clearPlaybackHistory() = Unit
    }
}
