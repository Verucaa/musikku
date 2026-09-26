package com.zaaam.liphify.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.exoplayer.ExoPlayer

/** PRD-002/004/006: Media3 MediaSessionService — satu engine untuk lokal + YouTube. */
class LiPhifySessionService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val p = ExoPlayer.Builder(this).build()
        player = p
        session = MediaSession.Builder(this, p).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }

    companion object {
        fun buildMediaItem(
            key: String,
            title: String,
            artist: String,
            artworkUri: String?,
            streamUrl: String,
        ): MediaItem {
            val meta = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
                .build()
            return MediaItem.Builder()
                .setMediaId(key)
                .setUri(streamUrl)
                .setMediaMetadata(meta)
                .build()
        }
    }
}
