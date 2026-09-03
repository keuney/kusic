package com.keuney.music.core.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
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
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updatePlayback()
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
            })
            .buildAsync()
        pending = future
        future.addListener({
            if (pending !== future) return@addListener
            try {
                controller = future.get()
                if (controller?.isConnected == true) {
                    mutableState.value = ConnectionState.Connected
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
        mutableState.value = ConnectionState.Disconnected
        mutablePlayback.value = PlaybackState()
        future?.let(MediaController::releaseFuture)
    }

    @MainThread
    fun play() {
        checkMainThread()
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

    /** Gate 검증과 이후 큐 작업의 최소 진입점. 대기열에도 Track ID와 metadata만 넣는다. */
    @MainThread
    fun playQueue(tracks: List<Triple<String, String, String>>) {
        checkMainThread()
        if (tracks.isEmpty()) return
        val player = controller ?: return
        player.setMediaItems(
            tracks.map { (id, title, artist) ->
                MediaItem.Builder()
                    .setMediaId(id)
                    .setMediaMetadata(trackMetadata(title, artist, artworkUri = null))
                    .build()
            },
        )
        player.prepare()
        player.play()
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

    /**
     * 이전. 한 곡만 있을 때 Media3는 그 곡의 처음으로 되돌린다. 알림·잠금화면의 이전 버튼도
     * 같은 명령을 쓰므로 세 곳의 동작이 갈리지 않는다.
     */
    @MainThread
    fun seekToPrevious() {
        checkMainThread()
        val player = controller ?: return
        if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)) player.seekToPrevious()
    }

    /** 다음. 다음 곡이 없으면 명령 자체가 없으므로 아무 일도 하지 않는다. */
    @MainThread
    fun seekToNext() {
        checkMainThread()
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
        val item = player.currentMediaItem
        mutablePlayback.value = mapPlaybackState(
            playerState = player.playbackState,
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            positionMs = player.currentPosition,
            durationMs = player.duration,
            hasError = player.playerError != null,
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
        )
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper())
    }
}
