package com.ashishkudale.musicapp.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ashishkudale.musicapp.data.database.entities.PlaylistSong
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.service.MusicPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var musicPlayerService: MusicPlayerService? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    fun setService(service: MusicPlayerService) {
        musicPlayerService = service
    }

    fun playSong(song: Song, playlistSong: PlaylistSong? = null) {
        _currentSong.value = song
        musicPlayerService?.playSong(song, playlistSong)
        _isPlaying.value = true
        updatePlaybackInfo()
    }

    fun previewTimestamp(song: Song, startTimestamp: Long, endTimestamp: Long) {
        val playlistSong = PlaylistSong(
            id = 0,
            playlistId = 0,
            songId = song.id,
            position = 0,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            addedAt = System.currentTimeMillis()
        )
        playSong(song, playlistSong)
    }

    fun pausePlayback() {
        musicPlayerService?.pause()
        _isPlaying.value = false
    }

    fun resumePlayback() {
        musicPlayerService?.resume()
        _isPlaying.value = true
    }

    fun seekTo(position: Long) {
        musicPlayerService?.seekTo(position)
        _currentPosition.value = position
    }

    fun updatePlaybackInfo() {
        musicPlayerService?.let { service ->
            _isPlaying.value = service.isPlaying()
            _currentPosition.value = service.getCurrentPosition()
            _duration.value = service.getDuration()
        }
    }
}
