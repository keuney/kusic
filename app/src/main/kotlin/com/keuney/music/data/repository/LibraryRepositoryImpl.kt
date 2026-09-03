package com.keuney.music.data.repository

import com.keuney.music.core.database.dao.FavoriteDao
import com.keuney.music.core.database.dao.PlaybackHistoryDao
import com.keuney.music.core.database.dao.PlaylistDao
import com.keuney.music.core.database.dao.PlaylistWithCount
import com.keuney.music.core.database.dao.TrackDao
import com.keuney.music.core.database.entity.FavoriteEntity
import com.keuney.music.core.database.entity.PlaybackHistoryEntity
import com.keuney.music.core.database.entity.TrackEntity
import com.keuney.music.core.library.LibraryRepository
import com.keuney.music.core.model.Playlist
import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * DAO 네 개를 라이브러리 경계 하나로 묶는다.
 *
 * 쓰기 전에 곡을 먼저 저장한다. 세 표가 모두 tracks를 외래 키로 가리키므로 곡이 없으면 쓰기가
 * 실패한다. 부르는 쪽이 그 순서를 알아야 할 이유가 없다.
 *
 * 시각은 밖에서 받지 않고 여기서 읽는다. 화면이 시계를 넘기게 만들면 화면마다 다른 시계를 쓸 수 있다.
 */
internal class LibraryRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val now: () -> Long = System::currentTimeMillis,
) : LibraryRepository {
    override val favorites: Flow<List<Track>> =
        favoriteDao.observeAll().map { it.map(TrackEntity::toTrack) }.distinctUntilChanged()

    override fun isFavorite(trackId: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(trackId).distinctUntilChanged()

    override suspend fun setFavorite(track: Track, favorite: Boolean) {
        if (favorite) {
            trackDao.upsert(track.toEntity())
            favoriteDao.add(FavoriteEntity(trackId = track.id, addedAt = now()))
        } else {
            favoriteDao.remove(track.id)
            // 즐겨찾기를 지운 곡이 어디에서도 쓰이지 않으면 메타데이터도 남길 이유가 없다.
            trackDao.deleteUnreferenced()
        }
    }

    override val playlists: Flow<List<Playlist>> =
        playlistDao.observeAll().map { it.map(PlaylistWithCount::toPlaylist) }.distinctUntilChanged()

    override fun playlistTracks(playlistId: Long): Flow<List<Track>> =
        playlistDao.observeTracks(playlistId).map { it.map(TrackEntity::toTrack) }.distinctUntilChanged()

    override suspend fun createPlaylist(name: String): Long = playlistDao.create(name, now())

    override suspend fun renamePlaylist(playlistId: Long, name: String) =
        playlistDao.rename(playlistId, name)

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.delete(playlistId)
        trackDao.deleteUnreferenced()
    }

    override suspend fun addToPlaylist(playlistId: Long, track: Track) {
        trackDao.upsert(track.toEntity())
        playlistDao.addTrack(playlistId, track.id)
    }

    override suspend fun removeFromPlaylist(playlistId: Long, trackId: String) {
        playlistDao.removeTrack(playlistId, trackId)
        trackDao.deleteUnreferenced()
    }

    override fun recentlyPlayed(limit: Int): Flow<List<Track>> =
        playbackHistoryDao.observeRecent(limit).map { it.map(TrackEntity::toTrack) }.distinctUntilChanged()

    override suspend fun recordPlayback(track: Track) {
        trackDao.upsert(track.toEntity())
        playbackHistoryDao.record(PlaybackHistoryEntity(trackId = track.id, playedAt = now()))
    }

    override suspend fun clearPlaybackHistory() {
        playbackHistoryDao.clear()
        trackDao.deleteUnreferenced()
    }
}

/** 재생 주소는 저장하지 않는다(AGENTS.md 8). 표에 담기는 것은 표시용 값과 곡 ID뿐이다. */
internal fun Track.toEntity() = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    artworkUrl = artworkUrl,
    durationMs = durationMs,
    source = source.name,
)

/** 알 수 없는 source 이름은 Remote로 본다. 외부 콘텐츠가 v0.1의 유일한 출처다. */
internal fun TrackEntity.toTrack() = Track(
    id = id,
    title = title,
    artist = artist,
    artworkUrl = artworkUrl,
    durationMs = durationMs,
    source = SourceType.entries.firstOrNull { it.name == source } ?: SourceType.Remote,
)

internal fun PlaylistWithCount.toPlaylist() = Playlist(id = id, name = name, trackCount = trackCount)
