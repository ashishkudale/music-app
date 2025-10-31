package com.ashishkudale.musicapp.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.data.database.entities.PlaylistSong

class MusicPlayerService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    private val binder = MusicBinder()
    private var currentPlaylistSong: PlaylistSong? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()

        // Listen for playback position updates
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    checkTimestampBoundary()
                }
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun playSong(song: Song, playlistSong: PlaylistSong? = null) {
        currentPlaylistSong = playlistSong

        val mediaItem = MediaItem.fromUri(Uri.parse(song.filePath))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        // Seek to start timestamp if specified
        if (playlistSong != null && playlistSong.startTimestamp > 0) {
            exoPlayer.seekTo(playlistSong.startTimestamp)
        }

        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        exoPlayer.play()
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    fun isPlaying(): Boolean {
        return exoPlayer.isPlaying
    }

    fun getCurrentPosition(): Long {
        return exoPlayer.currentPosition
    }

    fun getDuration(): Long {
        return exoPlayer.duration
    }

    fun getPlayer(): ExoPlayer {
        return exoPlayer
    }

    private fun checkTimestampBoundary() {
        currentPlaylistSong?.let { playlistSong ->
            if (playlistSong.endTimestamp > 0) {
                val currentPosition = exoPlayer.currentPosition

                if (currentPosition >= playlistSong.endTimestamp) {
                    // Stop or move to next song
                    onTimestampReached()
                }
            }
        }
    }

    private fun onTimestampReached() {
        // Handle end of timestamp segment
        exoPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}
