package com.keuney.music.data.repository

import com.keuney.music.core.database.dao.FavoriteDao
import com.keuney.music.core.database.dao.PlaybackHistoryDao
import com.keuney.music.core.database.dao.PlaylistDao
import com.keuney.music.core.database.dao.PlaylistWithCount
import com.keuney.music.core.database.dao.TrackDao
import com.keuney.music.core.database.entity.FavoriteEntity
import com.keuney.music.core.database.entity.PlaybackHistoryEntity
import com.keuney.music.core.database.entity.PlaylistItemEntity
import com.keuney.music.core.database.entity.TrackEntity
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KM-111: 라이브러리 경계가 DAO를 감추고 도메인 타입으로만 말하는지 확인한다.
 * 실제 SQL 동작은 LibraryDaoTest가 기기에서 다룬다.
 */
class LibraryRepositoryImplTest {
    private val trackDao = FakeTrackDao()
    private val favoriteDao = FakeFavoriteDao()
    private val playlistDao = FakePlaylistDao()
    private val historyDao = FakeHistoryDao()
    private val repository = LibraryRepositoryImpl(trackDao, favoriteDao, playlistDao, historyDao) { NOW }

    @Test
    fun favoritingSavesTheTrackFirst() = runBlocking {
        repository.setFavorite(track("a"), favorite = true)

        // 세 표가 tracks를 외래 키로 가리키므로 곡이 먼저 있어야 한다.
        assertEquals(listOf("a"), trackDao.saved.map(TrackEntity::id))
        assertEquals(listOf(FavoriteEntity("a", NOW)), favoriteDao.rows)
    }

    @Test
    fun unfavoritingRemovesTheRowAndTidiesUpTheTrack() = runBlocking {
        repository.setFavorite(track("a"), favorite = true)

        repository.setFavorite(track("a"), favorite = false)

        assertTrue(favoriteDao.rows.isEmpty())
        assertTrue("쓰이지 않는 곡을 정리하지 않았다", trackDao.tidiedUp)
    }

    @Test
    fun favoritesComeBackAsDomainTracks() = runBlocking {
        favoriteDao.rows += FavoriteEntity("a", NOW)
        favoriteDao.tracks.value = listOf(track("a").toEntity())

        assertEquals(listOf(track("a")), repository.favorites.first())
    }

    @Test
    fun theFavoriteFlagIsObserved() = runBlocking {
        assertFalse(repository.isFavorite("a").first())

        favoriteDao.rows += FavoriteEntity("a", NOW)

        assertTrue(repository.isFavorite("a").first())
    }

    @Test
    fun playlistsCarryTheirTrackCount() = runBlocking {
        playlistDao.playlists.value = listOf(PlaylistWithCount(1, "출근길", 3))

        val playlists = repository.playlists.first()

        assertEquals(1, playlists.single().id)
        assertEquals("출근길", playlists.single().name)
        assertEquals(3, playlists.single().trackCount)
    }

    @Test
    fun creatingAPlaylistStampsTheTime() = runBlocking {
        assertEquals(1L, repository.createPlaylist("출근길"))

        assertEquals(listOf("출근길" to NOW), playlistDao.created)
    }

    @Test
    fun addingToAPlaylistSavesTheTrackFirst() = runBlocking {
        repository.addToPlaylist(playlistId = 1, track = track("a"))

        assertEquals(listOf("a"), trackDao.saved.map(TrackEntity::id))
        assertEquals(listOf(1L to "a"), playlistDao.items.map { it.playlistId to it.trackId })
    }

    @Test
    fun recordingPlaybackSavesTheTrackAndStampsTheTime() = runBlocking {
        repository.recordPlayback(track("a"))

        assertEquals(listOf("a"), trackDao.saved.map(TrackEntity::id))
        assertEquals(listOf("a" to NOW), historyDao.rows.map { it.trackId to it.playedAt })
    }

    @Test
    fun clearingHistoryAlsoTidiesUpTracks() = runBlocking {
        repository.recordPlayback(track("a"))

        repository.clearPlaybackHistory()

        assertTrue(historyDao.rows.isEmpty())
        assertTrue(trackDao.tidiedUp)
    }

    @Test
    fun anUnknownStoredSourceIsReadAsRemote() {
        val stored = track("a").toEntity().copy(source = "future-source")

        assertEquals(SourceType.Remote, stored.toTrack().source)
    }

    @Test
    fun aStoredTrackKeepsItsMetadataThroughARoundTrip() {
        val original = Track("a", "제목", "아티스트", "https://example.invalid/a.jpg", 1_000, SourceType.Remote)

        assertEquals(original, original.toEntity().toTrack())
    }

    private fun track(id: String) =
        Track(id, "제목 $id", "아티스트", null, 180_000, SourceType.Remote)

    private class FakeTrackDao : TrackDao {
        val saved = mutableListOf<TrackEntity>()
        var tidiedUp = false
            private set

        override suspend fun upsert(track: TrackEntity) {
            saved.removeAll { it.id == track.id }
            saved += track
        }

        override suspend fun find(trackId: String) = saved.firstOrNull { it.id == trackId }

        override suspend fun deleteUnreferenced() {
            tidiedUp = true
        }
    }

    private class FakeFavoriteDao : FavoriteDao {
        val rows = mutableListOf<FavoriteEntity>()
        val tracks = MutableStateFlow(emptyList<TrackEntity>())

        override fun observeAll(): Flow<List<TrackEntity>> = tracks

        override fun observeIsFavorite(trackId: String): Flow<Boolean> =
            tracks.map { rows.any { row -> row.trackId == trackId } }

        override suspend fun add(favorite: FavoriteEntity) {
            rows.removeAll { it.trackId == favorite.trackId }
            rows += favorite
        }

        override suspend fun remove(trackId: String) {
            rows.removeAll { it.trackId == trackId }
        }
    }

    private class FakePlaylistDao : PlaylistDao {
        val playlists = MutableStateFlow(emptyList<PlaylistWithCount>())
        val created = mutableListOf<Pair<String, Long>>()
        val items = mutableListOf<PlaylistItemEntity>()

        override fun observeAll(): Flow<List<PlaylistWithCount>> = playlists

        override fun observeTracks(playlistId: Long): Flow<List<TrackEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun create(name: String, createdAt: Long): Long {
            created += name to createdAt
            return created.size.toLong()
        }

        override suspend fun rename(playlistId: Long, name: String) = Unit

        override suspend fun delete(playlistId: Long) {
            items.removeAll { it.playlistId == playlistId }
        }

        override suspend fun addItem(item: PlaylistItemEntity) {
            items += item
        }

        override suspend fun nextPosition(playlistId: Long) =
            items.count { it.playlistId == playlistId }

        override suspend fun removeTrack(playlistId: Long, trackId: String) {
            items.removeAll { it.playlistId == playlistId && it.trackId == trackId }
        }
    }

    private class FakeHistoryDao : PlaybackHistoryDao {
        val rows = mutableListOf<PlaybackHistoryEntity>()

        override fun observeRecent(limit: Int): Flow<List<TrackEntity>> = MutableStateFlow(emptyList())

        // 실제 DAO는 이 둘을 한 트랜잭션으로 묶는다. 가짜에서도 곡별 한 행만 남긴다.
        override suspend fun deleteFor(trackId: String) {
            rows.removeAll { it.trackId == trackId }
        }

        override suspend fun insert(entry: PlaybackHistoryEntity) {
            rows += entry
        }

        override suspend fun clear() = rows.clear()
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
