package com.ashishkudale.musicapp.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepository = MusicRepository(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                musicRepository.loadSongs()
                // Get the current value from the flow instead of collecting indefinitely
                musicRepository.songs.collect { songList ->
                    _songs.value = songList
                    _isLoading.value = false
                    // Stop collecting after first emission
                    return@collect
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    fun searchSongs(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            viewModelScope.launch {
                musicRepository.songs.collect { songList ->
                    _songs.value = songList
                    return@collect
                }
            }
        } else {
            _songs.value = musicRepository.searchSongs(query)
        }
    }

    fun getSongById(id: Long): Song? {
        return musicRepository.getSongById(id)
    }
}
