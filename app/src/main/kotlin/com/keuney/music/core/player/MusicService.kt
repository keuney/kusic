package com.keuney.music.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaMetadata
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.keuney.music.R
import com.keuney.music.data.network.NetworkTimeouts
import com.keuney.music.MainActivity
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class MusicService : MediaLibraryService() {
    @Inject
    internal lateinit var trackStreamResolver: TrackStreamResolver

    @Inject
    internal lateinit var playbackCache: PlaybackCache

    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val servicePlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cachingDataSourceFactory()))
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .build()
        player = servicePlayer
        // progressive 스트림에는 영상 트랙이 함께 들어 있다. 음악 재생에는 필요하지 않으므로 끈다.
        servicePlayer.trackSelectionParameters = servicePlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
        servicePlayer.setMediaItem(localTestTone())
        session = MediaLibrarySession.Builder(
            this,
            servicePlayer,
            object : MediaLibrarySession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> =
                    Futures.immediateFuture(mediaItems.map(::withPlayableUri).toMutableList())
            },
        ).setSessionActivity(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
    }

    /** 컨트롤러가 보낸 MediaItem에는 URI가 없다. Track ID를 자리표시 URI로 되돌린다. */
    private fun withPlayableUri(item: MediaItem): MediaItem = when {
        item.localConfiguration != null -> item
        item.mediaId == TEST_TONE_MEDIA_ID -> localTestTone()
        else -> runCatching {
            item.buildUpon().setUri(TrackUri.of(item.mediaId)).build()
        }.getOrDefault(item)
    }

    /**
     * 캐시를 가장 바깥에 둔다. 자리표시 URI가 캐시 키가 되므로 매번 달라지는 스트림 주소와
     * 무관하게 같은 곡을 다시 쓸 수 있고, 캐시에 있으면 주소 해석 자체를 건너뛴다.
     */
    private fun cachingDataSourceFactory() = CacheDataSource.Factory()
        .setCache(playbackCache.cache)
        // 캐시 아래에 재해석 재시도를 둔다. 캐시 적중은 재시도 경로를 타지 않는다.
        .setUpstreamDataSourceFactory(RefreshingDataSource.Factory(resolvingDataSourceFactory()))
        // 기본 조각은 5MB라 곡을 짧게 듣고 멈추면 아무것도 남지 않는다. 더 자주 확정한다.
        .setCacheWriteDataSinkFactory(
            CacheDataSink.Factory()
                .setCache(playbackCache.cache)
                .setFragmentSize(PlaybackCache.FRAGMENT_BYTES),
        )
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    private fun resolvingDataSourceFactory() = ResolvingDataSource.Factory(
        DefaultDataSource.Factory(
            this,
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(NetworkTimeouts.CONNECT_MS)
                .setReadTimeoutMs(NetworkTimeouts.PLAYBACK_READ_MS),
        ),
        trackStreamResolver,
    )

    private fun localTestTone(): MediaItem = MediaItem.Builder()
        .setMediaId(TEST_TONE_MEDIA_ID)
        .setUri("android.resource://$packageName/${R.raw.test_tone}")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(getString(R.string.test_audio))
                .setArtist(getString(R.string.app_name))
                .setArtworkData(createPlaceholderArtwork(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build(),
        )
        .build()

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onDestroy() {
        player?.release()
        player = null
        session?.release()
        session = null
        super.onDestroy()
    }

    internal companion object {
        const val TEST_TONE_MEDIA_ID = "known-test-tone"
    }
}
