package com.keuney.music.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaMetadata
import com.keuney.music.R
import com.keuney.music.MainActivity
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

class MusicService : MediaLibraryService() {
    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val servicePlayer = ExoPlayer.Builder(this)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .build()
        player = servicePlayer
        servicePlayer.setMediaItem(
            MediaItem.Builder()
                .setMediaId("known-test-tone")
                .setUri("android.resource://$packageName/${R.raw.test_tone}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(getString(R.string.test_audio))
                        .setArtist(getString(R.string.app_name))
                        .setArtworkData(createPlaceholderArtwork(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build(),
                )
                .build(),
        )
        session = MediaLibrarySession.Builder(
            this,
            servicePlayer,
            object : MediaLibrarySession.Callback {},
        ).setSessionActivity(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onDestroy() {
        player?.release()
        player = null
        session?.release()
        session = null
        super.onDestroy()
    }
}
