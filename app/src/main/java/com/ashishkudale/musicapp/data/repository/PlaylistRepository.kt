package com.ashishkudale.musicapp.data.repository

import com.ashishkudale.musicapp.data.database.dao.PlaylistDao
import com.ashishkudale.musicapp.data.database.dao.PlaylistSongDao
import com.ashishkudale.musicapp.data.database.entities.Playlist
import com.ashishkudale.musicapp.data.database.entities.PlaylistSong
import com.ashishkudale.musicapp.data.database.entities.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val playlistSongDao: PlaylistSongDao
) {

    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists()
    }

    fun getPlaylist(playlistId: Long): Flow<Playlist?> {
        return playlistDao.getPlaylist(playlistId)
    }

    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> {
        return playlistDao.getPlaylistWithSongs(playlistId)
    }

    suspend fun createPlaylist(name: String, description: String?): Long {
        val playlist = Playlist(
            name = name,
            description = description,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            songCount = 0
        )
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }

    fun getSongsInPlaylist(playlistId: Long): Flow<List<PlaylistSong>> {
        return playlistSongDao.getSongsInPlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(
        playlistId: Long,
        songId: Long,
        position: Int,
        startTimestamp: Long = 0,
        endTimestamp: Long = -1
    ) {
        val playlistSong = PlaylistSong(
            playlistId = playlistId,
            songId = songId,
            position = position,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            addedAt = System.currentTimeMillis()
        )
        playlistSongDao.insertSong(playlistSong)
    }

    suspend fun updatePlaylistSong(playlistSong: PlaylistSong) {
        playlistSongDao.updateSong(playlistSong)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistSongDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderSongs(songs: List<PlaylistSong>) {
        playlistSongDao.reorderSongs(songs)
    }
}
