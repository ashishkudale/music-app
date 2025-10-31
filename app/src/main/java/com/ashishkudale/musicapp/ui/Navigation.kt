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
}
