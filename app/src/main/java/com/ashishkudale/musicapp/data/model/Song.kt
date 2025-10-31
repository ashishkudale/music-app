package com.ashishkudale.musicapp.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val filePath: String,
    val albumArtUri: String?,
    val dateAdded: Long
)
