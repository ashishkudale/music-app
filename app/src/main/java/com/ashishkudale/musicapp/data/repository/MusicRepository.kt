package com.ashishkudale.musicapp.data.repository

import android.content.Context
import android.util.Log
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.util.MusicScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicRepository(private val context: Context) {

    private val musicScanner = MusicScanner(context)
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: Flow<List<Song>> = _songs.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: Flow<String?> = _error.asStateFlow()

    suspend fun loadSongs() {
        try {
            val scannedSongs = musicScanner.scanMusicFiles()
            _songs.value = scannedSongs
            _error.value = null
            Log.d("MusicRepository", "Loaded ${scannedSongs.size} songs")
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error loading songs", e)
            _error.value = "Failed to load music files: ${e.message}"
        }
    }

    fun getSongById(id: Long): Song? {
        return _songs.value.firstOrNull { it.id == id }
    }

    fun searchSongs(query: String): List<Song> {
        if (query.isBlank()) return _songs.value

        return _songs.value.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
