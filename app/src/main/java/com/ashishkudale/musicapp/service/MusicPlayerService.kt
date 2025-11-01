package com.ashishkudale.musicapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.ashishkudale.musicapp.MainActivity
import com.ashishkudale.musicapp.R
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.data.database.entities.PlaylistSong

class MusicPlayerService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val binder = MusicBinder()
    private var currentSong: Song? = null
    private var currentPlaylistSong: PlaylistSong? = null
    private val handler = Handler(Looper.getMainLooper())
    private val positionCheckInterval = 100L // Check every 100ms

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_playback_channel"
        const val ACTION_PLAY = "com.ashishkudale.musicapp.ACTION_PLAY"
        const val ACTION_PAUSE = "com.ashishkudale.musicapp.ACTION_PAUSE"
        const val ACTION_NEXT = "com.ashishkudale.musicapp.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.ashishkudale.musicapp.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.ashishkudale.musicapp.ACTION_STOP"
    }

    private val positionCheckRunnable = object : Runnable {
        override fun run() {
            if (exoPlayer.isPlaying) {
                checkTimestampBoundary()
                handler.postDelayed(this, positionCheckInterval)
            }
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()

        // Create MediaSession
        mediaSession = MediaSession.Builder(this, exoPlayer).build()

        // Create notification channel
        createNotificationChannel()

        // Listen for playback state changes
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // Start position checking when playback starts
                    handler.removeCallbacks(positionCheckRunnable)
                    handler.post(positionCheckRunnable)
                } else {
                    // Stop position checking when playback pauses
                    handler.removeCallbacks(positionCheckRunnable)
                }
                // Update notification
                updateNotification()
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun playSong(song: Song, playlistSong: PlaylistSong? = null) {
        currentSong = song
        currentPlaylistSong = playlistSong

        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(song.filePath))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            // Seek to start timestamp if specified
            if (playlistSong != null && playlistSong.startTimestamp > 0) {
                exoPlayer.seekTo(playlistSong.startTimestamp)
            }

            exoPlayer.play()
            startForegroundService()
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error playing song", e)
        }
    }

    private fun startForegroundService() {
        try {
            if (currentSong != null) {
                val notification = createNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error starting foreground service", e)
        }
    }

    private fun createNotification(): android.app.Notification {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val playPauseAction = if (exoPlayer.isPlaying) {
                NotificationCompat.Action(
                    R.drawable.ic_pause,
                    "Pause",
                    getPendingIntent(ACTION_PAUSE)
                )
            } else {
                NotificationCompat.Action(
                    R.drawable.ic_play,
                    "Play",
                    getPendingIntent(ACTION_PLAY)
                )
            }

            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSong?.title ?: "Music Player")
                .setContentText(currentSong?.artist ?: "No song playing")
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .addAction(playPauseAction)
                .addAction(
                    R.drawable.ic_stop,
                    "Stop",
                    getPendingIntent(ACTION_STOP)
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error creating notification", e)
            // Return a basic notification as fallback
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Music Player")
                .setContentText("Playing")
                .setSmallIcon(R.drawable.ic_music_note)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    private fun updateNotification() {
        if (currentSong != null) {
            val notification = createNotification()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
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
            // Check if end timestamp is set (not -1)
            if (playlistSong.endTimestamp > 0) {
                val currentPosition = exoPlayer.currentPosition

                // If we've reached or passed the end timestamp, stop playback
                if (currentPosition >= playlistSong.endTimestamp) {
                    onTimestampReached()
                }
            }
        }
    }

    private fun onTimestampReached() {
        // Handle end of timestamp segment
        handler.removeCallbacks(positionCheckRunnable)
        exoPlayer.pause()
        // Reset to start timestamp for replay
        currentPlaylistSong?.let {
            if (it.startTimestamp > 0) {
                exoPlayer.seekTo(it.startTimestamp)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            handler.removeCallbacks(positionCheckRunnable)
            if (::mediaSession.isInitialized) {
                mediaSession.release()
            }
            if (::exoPlayer.isInitialized) {
                exoPlayer.release()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerService", "Error in onDestroy", e)
        }
    }
}
