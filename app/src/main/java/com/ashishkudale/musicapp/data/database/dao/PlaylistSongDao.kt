package com.ashishkudale.musicapp.data.database.dao

import androidx.room.*
import com.ashishkudale.musicapp.data.database.entities.PlaylistSong
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongDao {
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    fun getSongsInPlaylist(playlistId: Long): Flow<List<PlaylistSong>>

    @Insert
    suspend fun insertSong(playlistSong: PlaylistSong)

    @Update
    suspend fun updateSong(playlistSong: PlaylistSong)

    @Delete
    suspend fun deleteSong(playlistSong: PlaylistSong)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Transaction
    suspend fun reorderSongs(songs: List<PlaylistSong>) {
        songs.forEach { updateSong(it) }
    }
}
