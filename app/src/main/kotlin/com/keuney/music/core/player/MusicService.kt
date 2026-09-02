package com.keuney.music.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaMetadata
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.keuney.music.R
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

    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val servicePlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingDataSourceFactory()))
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

    /** http(s)만 청크 요청으로 감싼다. 내장 테스트 음원 같은 로컬 스킴은 기본 경로를 쓴다. */
    private fun resolvingDataSourceFactory() = ResolvingDataSource.Factory(
        DefaultDataSource.Factory(
            this,
            ChunkedHttpDataSource.Factory(
                DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true),
            ),
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
