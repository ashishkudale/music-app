package com.ashishkudale.musicapp.ui

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Player : Screen("player/{songId}") {
        fun createRoute(songId: Long) = "player/$songId"
    }
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    object TimestampEditor : Screen("timestamp/{playlistId}/{songId}") {
        fun createRoute(playlistId: Long, songId: Long) = "timestamp/$playlistId/$songId"
    }
}
