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
import kotlinx.coroutines.flow.first
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

    fun loadPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("PlaylistViewModel", "Loading playlist $playlistId")
                playlistRepository.getPlaylistWithSongs(playlistId).collect { playlistWithSongsData ->
                    _playlistWithSongs.value = playlistWithSongsData
                    _currentPlaylist.value = playlistWithSongsData?.playlist

                    android.util.Log.d("PlaylistViewModel", "Playlist data: ${playlistWithSongsData?.playlist?.name}, songs in DB: ${playlistWithSongsData?.songs?.size ?: 0}")

                    // Load song details
                    playlistWithSongsData?.songs?.let { playlistSongs ->
                        android.util.Log.d("PlaylistViewModel", "Loading details for ${playlistSongs.size} songs")
                        val songsWithDetails = playlistSongs.map { playlistSong ->
                            val song = musicRepository.getSongById(playlistSong.songId)
                            android.util.Log.d("PlaylistViewModel", "PlaylistSong ID: ${playlistSong.id}, SongID: ${playlistSong.songId}, Found: ${song != null}, Title: ${song?.title}")
                            Pair(playlistSong, song)
                        }
                        _playlistSongsWithDetails.value = songsWithDetails
                        android.util.Log.d("PlaylistViewModel", "Set ${songsWithDetails.size} songs with details")
                    } ?: run {
                        _playlistSongsWithDetails.value = emptyList()
                        android.util.Log.d("PlaylistViewModel", "No songs in playlist")
                    }

                    _isLoading.value = false
                    // Stop collecting after first emission
                    return@collect
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("PlaylistViewModel", "Error loading playlist: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    fun createPlaylist(name: String, description: String?) {
        viewModelScope.launch {
            try {
                playlistRepository.createPlaylist(name, description)
                hideCreateDialog()
                // Reload playlists to show the new one
                loadPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("PlaylistViewModel", "Error creating playlist", e)
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
                // Reload playlists to update the list
                loadPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("PlaylistViewModel", "Error deleting playlist", e)
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            android.util.Log.d("PlaylistViewModel", "Adding song ${song.title} (ID: ${song.id}) to playlist $playlistId")

            try {
                // Get current max position
                android.util.Log.d("PlaylistViewModel", "Step 1: Getting max position")
                val currentSongs = playlistRepository.getSongsInPlaylist(playlistId).first()
                val maxPosition = currentSongs.maxOfOrNull { it.position } ?: -1
                android.util.Log.d("PlaylistViewModel", "Current max position: $maxPosition, existing songs: ${currentSongs.size}")

                android.util.Log.d("PlaylistViewModel", "Step 2: About to insert song at position ${maxPosition + 1}")

                // Add song to playlist
                playlistRepository.addSongToPlaylist(
                    playlistId = playlistId,
                    songId = song.id,
                    position = maxPosition + 1
                )
                android.util.Log.d("PlaylistViewModel", "Step 3: Song added to database successfully")

                // Wait a bit for database to complete
                android.util.Log.d("PlaylistViewModel", "Step 4: Waiting for DB to complete")
                kotlinx.coroutines.delay(200)

                android.util.Log.d("PlaylistViewModel", "Step 5: Updating playlist song count")
                // Update song count
                val playlist = playlistRepository.getPlaylist(playlistId).first()
                playlist?.let {
                    playlistRepository.updatePlaylist(it.copy(songCount = it.songCount + 1))
                    android.util.Log.d("PlaylistViewModel", "Updated playlist song count to ${it.songCount + 1}")
                }

                android.util.Log.d("PlaylistViewModel", "Step 6: Reloading playlists")
                // Reload playlists and current playlist
                loadPlaylists()

                android.util.Log.d("PlaylistViewModel", "Step 7: Checking if need to reload current playlist")
                if (_currentPlaylist.value?.id == playlistId) {
                    android.util.Log.d("PlaylistViewModel", "Step 7a: Reloading current playlist $playlistId")
                    loadPlaylist(playlistId)
                }

                android.util.Log.d("PlaylistViewModel", "Step 8: COMPLETE - All steps finished successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("PlaylistViewModel", "FATAL ERROR in addSongToPlaylist: ${e.message}", e)
                android.util.Log.e("PlaylistViewModel", "Stack trace: ${e.stackTraceToString()}")
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

    fun updateSongTimestamp(playlistSongId: Long, startTimestamp: Long, endTimestamp: Long) {
        viewModelScope.launch {
            try {
                // Find the playlist song and update it
                _playlistSongsWithDetails.value.find { it.first.id == playlistSongId }?.let { (playlistSong, _) ->
                    val updatedSong = playlistSong.copy(
                        startTimestamp = startTimestamp,
                        endTimestamp = endTimestamp
                    )
                    playlistRepository.updatePlaylistSong(updatedSong)

                    // Reload the current playlist to reflect changes
                    _currentPlaylist.value?.let { playlist ->
                        loadPlaylist(playlist.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getPlaylistSongById(playlistSongId: Long): PlaylistSong? {
        return _playlistSongsWithDetails.value.find { it.first.id == playlistSongId }?.first
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
