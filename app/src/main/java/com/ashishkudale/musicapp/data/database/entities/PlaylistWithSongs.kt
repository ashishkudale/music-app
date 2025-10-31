package com.ashishkudale.musicapp.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val songs: List<PlaylistSong>
)
