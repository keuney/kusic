package com.keuney.music.core.database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.keuney.music.core.database.entity.FavoriteEntity
import com.keuney.music.core.database.entity.PlaybackHistoryEntity
import com.keuney.music.core.database.entity.TrackEntity
import com.keuney.music.data.repository.LibraryRepositoryImpl
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * KM-111: 실제 SQL과 외래 키 동작을 확인한다. 관찰이 Flow로 스스로 갱신되는지도 여기서 본다.
 *
 * 메모리 데이터베이스를 쓴다. 기기의 실제 파일을 건드리지 않으며 검사마다 새로 만든다.
 */
class LibraryDaoTest {
    private lateinit var database: KeuneyDatabase
    private lateinit var repository: LibraryRepositoryImpl
    private var clock = 1_000L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, KeuneyDatabase::class.java)
            // 외래 키는 SQLite 기본이 꺼짐이다. 실제 앱과 같게 켠다.
            .setQueryCallback({ _, _ -> }) { it.run() }
            .build()
        database.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = ON")
        repository = LibraryRepositoryImpl(
            database.trackDao(),
            database.favoriteDao(),
            database.playlistDao(),
            database.playbackHistoryDao(),
        ) { clock }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun favoritesAreObservedNewestFirst(): Unit = runBlocking {
        assertEquals(emptyList<Track>(), repository.favorites.first())

        clock = 1_000
        repository.setFavorite(track("a"), favorite = true)
        clock = 2_000
        repository.setFavorite(track("b"), favorite = true)

        assertEquals(listOf("b", "a"), repository.favorites.first().map(Track::id))
        assertTrue(repository.isFavorite("a").first())
        assertFalse(repository.isFavorite("c").first())

        repository.setFavorite(track("a"), favorite = false)
        assertEquals(listOf("b"), repository.favorites.first().map(Track::id))
    }

    @Test
    fun favoritingTheSameTrackTwiceKeepsOneRow(): Unit = runBlocking {
        repository.setFavorite(track("a"), favorite = true)
        clock = 5_000
        repository.setFavorite(track("a"), favorite = true)

        assertEquals(listOf("a"), repository.favorites.first().map(Track::id))
    }

    @Test
    fun playlistTracksKeepTheOrderTheyWereAdded(): Unit = runBlocking {
        val id = repository.createPlaylist("출근길")

        repository.addToPlaylist(id, track("a"))
        repository.addToPlaylist(id, track("b"))
        repository.addToPlaylist(id, track("c"))

        assertEquals(listOf("a", "b", "c"), repository.playlistTracks(id).first().map(Track::id))
        assertEquals(3, repository.playlists.first().single().trackCount)

        repository.removeFromPlaylist(id, "b")
        assertEquals(listOf("a", "c"), repository.playlistTracks(id).first().map(Track::id))
    }

    @Test
    fun theSameTrackCanBeAddedTwiceToAPlaylist(): Unit = runBlocking {
        val id = repository.createPlaylist("출근길")

        repository.addToPlaylist(id, track("a"))
        repository.addToPlaylist(id, track("a"))

        assertEquals(listOf("a", "a"), repository.playlistTracks(id).first().map(Track::id))
        // 빼면 그 곡의 항목이 모두 빠진다.
        repository.removeFromPlaylist(id, "a")
        assertEquals(emptyList<String>(), repository.playlistTracks(id).first().map(Track::id))
    }

    @Test
    fun renamingAndDeletingAPlaylistWorks(): Unit = runBlocking {
        val id = repository.createPlaylist("출근길")
        repository.addToPlaylist(id, track("a"))

        repository.renamePlaylist(id, "퇴근길")
        assertEquals("퇴근길", repository.playlists.first().single().name)

        repository.deletePlaylist(id)
        assertEquals(emptyList<Long>(), repository.playlists.first().map { it.id })
        // 담긴 항목은 외래 키가 함께 지운다.
        assertEquals(emptyList<String>(), repository.playlistTracks(id).first().map(Track::id))
    }

    @Test
    fun recentlyPlayedListsEachTrackOnceNewestFirst(): Unit = runBlocking {
        clock = 1_000
        repository.recordPlayback(track("a"))
        clock = 2_000
        repository.recordPlayback(track("b"))
        clock = 3_000
        // 같은 곡을 다시 들어도 목록에는 한 번만 나오고 맨 앞으로 온다.
        repository.recordPlayback(track("a"))

        assertEquals(listOf("a", "b"), repository.recentlyPlayed(10).first().map(Track::id))
        assertEquals(listOf("a"), repository.recentlyPlayed(1).first().map(Track::id))

        repository.clearPlaybackHistory()
        assertEquals(emptyList<String>(), repository.recentlyPlayed(10).first().map(Track::id))
    }

    @Test
    fun deletingATrackTakesItsFavoriteWithIt(): Unit = runBlocking {
        database.trackDao().upsert(track("a").entity())
        database.favoriteDao().add(FavoriteEntity("a", 1_000))
        database.playbackHistoryDao().record(PlaybackHistoryEntity(trackId = "a", playedAt = 1_000))

        database.openHelper.writableDatabase.execSQL("DELETE FROM tracks WHERE id = 'a'")

        assertEquals(emptyList<Track>(), repository.favorites.first())
        assertEquals(emptyList<Track>(), repository.recentlyPlayed(10).first())
    }

    @Test
    fun aTrackStaysWhileSomethingStillPointsAtIt(): Unit = runBlocking {
        val id = repository.createPlaylist("출근길")
        repository.addToPlaylist(id, track("a"))
        repository.setFavorite(track("a"), favorite = true)

        // 즐겨찾기를 지워도 재생목록이 아직 그 곡을 가리킨다.
        repository.setFavorite(track("a"), favorite = false)

        assertEquals(listOf("a"), repository.playlistTracks(id).first().map(Track::id))
    }

    private fun track(id: String) =
        Track(id, "제목 $id", "아티스트", null, 180_000, SourceType.Remote)

    private fun Track.entity() = TrackEntity(id, title, artist, artworkUrl, durationMs, source.name)
}
