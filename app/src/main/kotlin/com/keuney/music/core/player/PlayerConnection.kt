package com.keuney.music.core.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.keuney.music.core.model.Track
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class ConnectionState { Disconnected, Connecting, Connected, Unavailable }

internal class PlayerConnection @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { mainHandler.post(it) }
    private var pending: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val mutableState = MutableStateFlow(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = mutableState.asStateFlow()
    private val mutablePlayback = MutableStateFlow(PlaybackState())
    val playback: StateFlow<PlaybackState> = mutablePlayback.asStateFlow()
    /** 대기열은 바뀔 때만 다시 읽는다. 위치 갱신은 250ms마다 오므로 매번 만들면 낭비다. */
    private var queue: List<NowPlaying> = emptyList()
    private var queueStale = true

    /**
     * 재생할 수 없어 지나간 곡의 제목과, 그것을 이미 본 기준이 되는 횟수(KM-138).
     *
     * 서비스가 세션 extras로 알려 온다. 컨트롤러가 스스로 알아낼 수는 없다. 오류가 나자마자
     * 서비스가 다음 곡으로 넘기고 다시 준비하므로, 여기에는 오류 상태가 도착하기도 전에 이미
     * 다음 곡이 재생되고 있다.
     *
     * 횟수를 보는 이유는 **새로 넘어간 것**만 알려야 하기 때문이다. 붙을 때 이미 있던 값은
     * 기준으로만 삼고 안내하지 않는다. 지난 일을 지금 일어난 것처럼 보여 주지 않는다.
     */
    private var seenSkipCount = 0
    private var skippedTitle: String? = null
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updatePlayback()

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            queueStale = true
        }
    }
    private val positionUpdate = object : Runnable {
        override fun run() {
            if (controller == null) return
            updatePlayback()
            mainHandler.postDelayed(this, 250)
        }
    }

    @MainThread
    fun connect() {
        checkMainThread()
        if (pending != null) return
        mutableState.value = ConnectionState.Connecting
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token)
            .setApplicationLooper(Looper.getMainLooper())
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    if (this@PlayerConnection.controller === controller) {
                        disconnect()
                        mutableState.value = ConnectionState.Unavailable
                    }
                }

                override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                    if (this@PlayerConnection.controller === controller) readSkipNote(extras)
                }
            })
            .buildAsync()
        pending = future
        future.addListener({
            if (pending !== future) return@addListener
            try {
                controller = future.get()
                if (controller?.isConnected == true) {
                    mutableState.value = ConnectionState.Connected
                    // 붙는 순간의 대기열은 알림 없이 이미 있다.
                    queueStale = true
                    // 붙기 전에 지나간 곡은 지금 일어난 일이 아니다. 기준만 맞춘다.
                    seenSkipCount = controller?.sessionExtras
                        ?.getInt(MusicService.EXTRA_SKIP_COUNT, 0) ?: 0
                    controller?.addListener(playerListener)
                    positionUpdate.run()
                } else {
                    disconnect()
                    mutableState.value = ConnectionState.Unavailable
                }
            } catch (_: CancellationException) {
                disconnect()
            } catch (_: ExecutionException) {
                disconnect()
                mutableState.value = ConnectionState.Unavailable
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                disconnect()
                mutableState.value = ConnectionState.Unavailable
            }
        }, mainExecutor)
    }

    @MainThread
    fun disconnect() {
        checkMainThread()
        val future = pending
        pending = null
        mainHandler.removeCallbacks(positionUpdate)
        controller?.removeListener(playerListener)
        controller = null
        queue = emptyList()
        queueStale = true
        mutableState.value = ConnectionState.Disconnected
        mutablePlayback.value = PlaybackState()
        future?.let(MediaController::releaseFuture)
    }

    @MainThread
    fun play() {
        checkMainThread()
        // 무엇을 재생할지 바꾸는 조작이다. 지나간 곡 안내는 여기서 지운다(KM-138).
        clearSkipNote()
        val player = controller ?: return
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        if (player.playbackState == Player.STATE_IDLE || player.playerError != null) player.prepare()
        player.play()
    }

    /**
     * 대기열에는 Track ID와 metadata만 보낸다. 실제 재생 주소는 서비스가 재생 직전에 해석한다.
     *
     * [artworkUri]는 앨범 이미지 주소이며 재생 주소가 아니다. 세션에 넣으면 알림과 잠금화면도
     * 같은 이미지를 쓴다.
     */
    @MainThread
    fun playTrack(trackId: String, title: String, artist: String, artworkUri: String? = null) {
        checkMainThread()
        clearSkipNote()
        val player = controller ?: return
        player.setMediaItem(
            MediaItem.Builder()
                .setMediaId(trackId)
                .setMediaMetadata(trackMetadata(title, artist, artworkUri))
                .build(),
        )
        player.prepare()
        player.play()
    }

    /**
     * 대기열을 통째로 갈아 끼우고 [startIndex]부터 재생한다. 대기열에도 Track ID와 표시용
     * metadata만 넣는다. 실제 재생 주소는 서비스가 항목을 열 때 해석한다.
     */
    @MainThread
    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        checkMainThread()
        clearSkipNote()
        if (tracks.isEmpty()) return
        val player = controller ?: return
        player.setMediaItems(
            tracks.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.id)
                    .setMediaMetadata(trackMetadata(track.title, track.artist, track.artworkUrl))
                    .build()
            },
            startIndex.coerceIn(tracks.indices),
            0,
        )
        player.prepare()
        player.play()
    }

    /** 대기열에서 그 자리의 곡으로 넘어간다. 목록에서 항목을 눌렀을 때 쓴다. */
    @MainThread
    fun seekToQueueItem(index: Int) {
        checkMainThread()
        clearSkipNote()
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekTo(index, 0)
        player.play()
    }

    /** 대기열에서 한 곡을 뺀다. 지금 재생 중인 곡을 빼면 Media3가 다음 곡으로 넘어간다. */
    @MainThread
    fun removeQueueItem(index: Int) {
        checkMainThread()
        val player = controller ?: return
        if (!player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) return
        if (index !in 0 until player.mediaItemCount) return
        player.removeMediaItem(index)
    }

    /** 대기열에서 한 곡을 다른 자리로 옮긴다. */
    @MainThread
    fun moveQueueItem(from: Int, to: Int) {
        checkMainThread()
        val player = controller ?: return
        if (!player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) return
        val count = player.mediaItemCount
        if (from !in 0 until count || to !in 0 until count || from == to) return
        player.moveMediaItem(from, to)
    }

    @MainThread
    fun currentMediaId(): String? {
        checkMainThread()
        return controller?.currentMediaItem?.mediaId
    }

    @MainThread
    fun pause() {
        checkMainThread()
        controller?.pause()
    }

    /** 셔플. 명령이 없으면 아무 일도 하지 않는다. */
    @MainThread
    fun setShuffleEnabled(enabled: Boolean) {
        checkMainThread()
        val player = controller ?: return
        if (player.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE)) {
            player.shuffleModeEnabled = enabled
        }
    }

    /**
     * 이전. 한 곡만 있을 때 Media3는 그 곡의 처음으로 되돌린다. 알림·잠금화면의 이전 버튼도
     * 같은 명령을 쓰므로 세 곳의 동작이 갈리지 않는다.
     */
    @MainThread
    fun seekToPrevious() {
        checkMainThread()
        clearSkipNote()
        val player = controller ?: return
        if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)) player.seekToPrevious()
    }

    /** 다음. 다음 곡이 없으면 명령 자체가 없으므로 아무 일도 하지 않는다. */
    @MainThread
    fun seekToNext() {
        checkMainThread()
        clearSkipNote()
        val player = controller ?: return
        if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)) player.seekToNext()
    }

    @MainThread
    fun seekTo(positionMs: Long) {
        checkMainThread()
        val player = controller ?: return
        if (player.duration > 0 && player.isCurrentMediaItemSeekable) {
            player.seekTo(positionMs.coerceIn(0, player.duration))
        }
    }

    /** https 주소만 넣는다. 그 밖의 스킴은 세션이 읽으려다 실패할 뿐이다. */
    private fun trackMetadata(title: String, artist: String, artworkUri: String?): MediaMetadata =
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .apply {
                artworkUri?.takeIf { it.startsWith("https://") }?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()

    private fun updatePlayback() {
        val player = controller ?: return
        if (queueStale) {
            queue = player.readQueue()
            queueStale = false
        }
        val item = player.currentMediaItem
        val failure = playbackFailureOf(player.playerError?.errorCode)
        mutablePlayback.value = mapPlaybackState(
            playerState = player.playbackState,
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            positionMs = player.currentPosition,
            durationMs = player.duration,
            failure = failure,
            nowPlaying = nowPlayingOf(
                mediaId = item?.mediaId,
                title = item?.mediaMetadata?.title?.toString(),
                artist = item?.mediaMetadata?.artist?.toString(),
                artworkUri = item?.mediaMetadata?.artworkUri?.toString(),
            ),
            repeatMode = player.repeatMode,
            shuffleEnabled = player.shuffleModeEnabled,
            hasPrevious = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS),
            hasNext = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT),
            queue = queue,
            queueIndex = player.currentMediaItemIndex,
            skippedTitle = skippedTitle,
        )
    }

    /** 세션이 알려 온 "지나간 곡". 처음 보는 횟수일 때만 안내로 삼는다. */
    private fun readSkipNote(extras: Bundle) {
        val count = extras.getInt(MusicService.EXTRA_SKIP_COUNT, 0)
        if (count == seenSkipCount) return
        seenSkipCount = count
        skippedTitle = extras.getString(MusicService.EXTRA_SKIPPED_TITLE)
        updatePlayback()
    }

    /**
     * 지나갔다는 안내를 지운다. **무엇을 재생할지 사용자가 바꾸면** 이미 본 것으로 본다.
     *
     * 시간이 지나면 사라지게 하지 않는다. 화면을 보고 있지 않은 사이에 사라지면 무엇이 빠졌는지
     * 영영 알 수 없다. 일시정지나 위치 이동으로는 지우지 않는다. 그것은 지금 곡에 대한 조작이고
     * 지나간 곡을 보았다는 뜻이 아니다.
     */
    private fun clearSkipNote() {
        skippedTitle = null
    }

    /** 대기열을 화면이 쓸 목록으로 읽는다. 넣은 순서 그대로다. */
    private fun Player.readQueue(): List<NowPlaying> {
        val timeline = currentTimeline
        if (timeline.isEmpty) return emptyList()
        val window = Timeline.Window()
        return (0 until timeline.windowCount).mapNotNull { index ->
            val item = timeline.getWindow(index, window).mediaItem
            nowPlayingOf(
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString(),
                artist = item.mediaMetadata.artist?.toString(),
                artworkUri = item.mediaMetadata.artworkUri?.toString(),
            )
        }
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper())
    }
}
