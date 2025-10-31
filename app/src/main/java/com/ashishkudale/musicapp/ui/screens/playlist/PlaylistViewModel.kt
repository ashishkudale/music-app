package com.ashishkudale.musicapp.ui.screens.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashishkudale.musicapp.data.database.AppDatabase
import com.ashishkudale.musicapp.data.database.entities.Playlist
import com.ashishkudale.musicapp.data.database.entities.PlaylistSong
import com.ashishkudale.musicapp.data.database.entities.PlaylistWithSongs
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.data.repository.MusicRepository
import com.ashishkudale.musicapp.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val playlistRepository = PlaylistRepository(
        database.playlistDao(),
        database.playlistSongDao()
    )
    private val musicRepository = MusicRepository(application)

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<Playlist?>(null)
    val currentPlaylist: StateFlow<Playlist?> = _currentPlaylist.asStateFlow()

    private val _playlistWithSongs = MutableStateFlow<PlaylistWithSongs?>(null)
    val playlistWithSongs: StateFlow<PlaylistWithSongs?> = _playlistWithSongs.asStateFlow()

    private val _playlistSongsWithDetails = MutableStateFlow<List<Pair<PlaylistSong, Song?>>>(emptyList())
    val playlistSongsWithDetails: StateFlow<List<Pair<PlaylistSong, Song?>>> = _playlistSongsWithDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _showAddToPlaylistDialog = MutableStateFlow(false)
    val showAddToPlaylistDialog: StateFlow<Boolean> = _showAddToPlaylistDialog.asStateFlow()

    private val _selectedSongForPlaylist = MutableStateFlow<Song?>(null)
    val selectedSongForPlaylist: StateFlow<Song?> = _selectedSongForPlaylist.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                playlistRepository.getAllPlaylists().collect { playlistList ->
                    _playlists.value = playlistList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                playlistRepository.getPlaylistWithSongs(playlistId).collect { playlistWithSongsData ->
                    _playlistWithSongs.value = playlistWithSongsData
                    _currentPlaylist.value = playlistWithSongsData?.playlist

                    // Load song details
                    playlistWithSongsData?.songs?.let { playlistSongs ->
                        val songsWithDetails = playlistSongs.map { playlistSong ->
                            val song = musicRepository.getSongById(playlistSong.songId)
                            Pair(playlistSong, song)
                        }
                        _playlistSongsWithDetails.value = songsWithDetails
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlaylist(name: String, description: String?) {
        viewModelScope.launch {
            try {
                playlistRepository.createPlaylist(name, description)
                hideCreateDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                playlistRepository.updatePlaylist(playlist)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                playlistRepository.deletePlaylist(playlist)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            try {
                val currentSongs = playlistRepository.getSongsInPlaylist(playlistId)
                var maxPosition = -1
                currentSongs.collect { songs ->
                    maxPosition = songs.maxOfOrNull { it.position } ?: -1
                    return@collect
                }

                playlistRepository.addSongToPlaylist(
                    playlistId = playlistId,
                    songId = song.id,
                    position = maxPosition + 1
                )

                // Update song count
                _playlists.value.find { it.id == playlistId }?.let { playlist ->
                    updatePlaylist(playlist.copy(songCount = playlist.songCount + 1))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            try {
                playlistRepository.removeSongFromPlaylist(playlistId, songId)

                // Update song count
                _playlists.value.find { it.id == playlistId }?.let { playlist ->
                    updatePlaylist(playlist.copy(songCount = maxOf(0, playlist.songCount - 1)))
                }

                // Reload playlist
                loadPlaylist(playlistId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reorderSongs(songs: List<PlaylistSong>) {
        viewModelScope.launch {
            try {
                val reorderedSongs = songs.mapIndexed { index, song ->
                    song.copy(position = index)
                }
                playlistRepository.reorderSongs(reorderedSongs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun showAddToPlaylistDialog(song: Song) {
        _selectedSongForPlaylist.value = song
        _showAddToPlaylistDialog.value = true
    }

    fun hideAddToPlaylistDialog() {
        _showAddToPlaylistDialog.value = false
        _selectedSongForPlaylist.value = null
    }
}
