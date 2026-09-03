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
import androidx.media3.common.Player
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.keuney.music.R
import com.keuney.music.core.library.LibraryRepository
import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.data.network.NetworkTimeouts
import com.keuney.music.MainActivity
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class MusicService : MediaLibraryService() {
    @Inject
    internal lateinit var trackStreamResolver: TrackStreamResolver

    @Inject
    internal lateinit var playbackCache: PlaybackCache

    @Inject
    internal lateinit var settings: SettingsRepository

    @Inject
    internal lateinit var library: LibraryRepository

    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null

    /** 저장된 설정 적용과 재생 기록을 위한 수명. onDestroy에서 끊는다. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 지금 듣고 있는 곡을 기록으로 남기기 위해 기다리는 중이거나 이미 끝난 작업. */
    private var historyJob: Job? = null

    /** 그 작업이 어느 곡을 위한 것인지. 이 값이 있고 작업도 있으면 그 곡은 이미 처리했다. */
    private var historyItemId: String? = null

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
        // 반복 모드는 저장된 설정이 곧 적용되는 값이다. 화면은 설정만 바꾸고 여기서 재생에 옮긴다.
        serviceScope.launch {
            settings.repeatMode.collectLatest { servicePlayer.repeatMode = it.toPlayerRepeatMode() }
        }
        // 기록은 재생을 소유한 이곳에서 남긴다. 화면이 닫혀도 배경 재생은 이어지기 때문이다.
        servicePlayer.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    scheduleHistory(player)
                }
            },
        )
    }

    /**
     * 지금 곡을 얼마 이상 들으면 기록으로 남긴다.
     *
     * 위치를 주기적으로 살피지 않고, 재생이 시작될 때 남은 시간만큼 기다린 뒤 여전히 같은 곡이
     * 재생 중인지 본다. 일시정지하거나 곡이 바뀌면 기다림을 접는다.
     *
     * "이미 남겼다"는 판단은 곡 ID가 아니라 **재생 위치**로 되돌린다. 위치가 기준보다 앞이면 새로
     * 듣기 시작한 것으로 보고 다시 남길 수 있게 한다. 곡 ID만 보면 같은 곡을 다시 골라 재생할 때
     * 기록이 갱신되지 않아 최근 재생이 옛 시각에 머문다. 같은 항목을 다시 걸면 Media3가 전환
     * 콜백을 주지 않으므로 전환에 기댈 수도 없다.
     */
    private fun scheduleHistory(player: Player) {
        val item = player.currentMediaItem
        val mediaId = item?.mediaId?.takeIf(String::isNotBlank)
        val threshold = PlaybackHistory.listenedThresholdMs(player.duration)
        if (mediaId != historyItemId || player.currentPosition < threshold) {
            historyJob?.cancel()
            historyJob = null
            historyItemId = null
        }
        if (mediaId == null || !player.isPlaying || historyJob != null) return
        val track = nowPlayingOf(
            mediaId = mediaId,
            title = item.mediaMetadata.title?.toString(),
            artist = item.mediaMetadata.artist?.toString(),
            artworkUri = item.mediaMetadata.artworkUri?.toString(),
        )?.toTrack(player.duration) ?: return
        historyItemId = mediaId
        historyJob = serviceScope.launch {
            val remaining = threshold - player.currentPosition
            if (remaining > 0) delay(remaining)
            // 기다리는 동안 멈췄거나 다른 곡으로 넘어갔으면 남기지 않는다.
            if (player.isPlaying && player.currentMediaItem?.mediaId == mediaId) {
                library.recordPlayback(track)
            }
        }
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
        historyJob?.cancel()
        serviceScope.cancel()
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

private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.Off -> Player.REPEAT_MODE_OFF
    RepeatMode.One -> Player.REPEAT_MODE_ONE
    RepeatMode.All -> Player.REPEAT_MODE_ALL
}
