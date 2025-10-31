package com.ashishkudale.musicapp.data.repository

import android.content.Context
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.util.MusicScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicRepository(private val context: Context) {

    private val musicScanner = MusicScanner(context)
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: Flow<List<Song>> = _songs.asStateFlow()

    suspend fun loadSongs() {
        val scannedSongs = musicScanner.scanMusicFiles()
        _songs.value = scannedSongs
    }

    fun getSongById(id: Long): Song? {
        return _songs.value.firstOrNull { it.id == id }
    }

    fun searchSongs(query: String): List<Song> {
        return _songs.value.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }
}
