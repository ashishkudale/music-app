package com.ashishkudale.musicapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ashishkudale.musicapp.service.MusicPlayerService
import com.ashishkudale.musicapp.ui.Screen
import com.ashishkudale.musicapp.ui.screens.library.MusicLibraryScreen
import com.ashishkudale.musicapp.ui.screens.library.MusicViewModel
import com.ashishkudale.musicapp.ui.screens.player.PlayerScreen
import com.ashishkudale.musicapp.ui.screens.player.PlayerViewModel
import com.ashishkudale.musicapp.ui.screens.playlist.PlaylistDetailScreen
import com.ashishkudale.musicapp.ui.screens.playlist.PlaylistScreen
import com.ashishkudale.musicapp.ui.screens.playlist.PlaylistViewModel
import com.ashishkudale.musicapp.ui.screens.timestamp.TimestampEditorScreen
import com.ashishkudale.musicapp.ui.theme.MusicAppTheme
import com.ashishkudale.musicapp.util.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private var musicPlayerService: MusicPlayerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicBinder
            musicPlayerService = binder.getService()
            playerViewModel.setService(binder.getService())
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only bind to music player service (don't start foreground until playing)
        Intent(this, MusicPlayerService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            MusicAppTheme {
                MusicPlayerApp(
                    musicViewModel = musicViewModel,
                    playerViewModel = playerViewModel,
                    playlistViewModel = playlistViewModel
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicPlayerApp(
    musicViewModel: MusicViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel
) {
    val navController = rememberNavController()

    // Request permissions
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (permissionsState.allPermissionsGranted) {
        // Add to Playlist Dialog - shown at app level
        val showAddToPlaylistDialog by playlistViewModel.showAddToPlaylistDialog.collectAsState()
        val selectedSong by playlistViewModel.selectedSongForPlaylist.collectAsState()
        val playlists by playlistViewModel.playlists.collectAsState()

        if (showAddToPlaylistDialog && selectedSong != null) {
            com.ashishkudale.musicapp.ui.screens.playlist.AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { playlistViewModel.hideAddToPlaylistDialog() },
                onPlaylistSelected = { playlist ->
                    selectedSong?.let { song ->
                        playlistViewModel.addSongToPlaylist(playlist.id, song)
                    }
                    playlistViewModel.hideAddToPlaylistDialog()
                }
            )
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "Library") },
                        label = { Text("Library") },
                        selected = navController.currentDestination?.route == Screen.Library.route,
                        onClick = {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(Screen.Library.route) { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlists") },
                        label = { Text("Playlists") },
                        selected = navController.currentDestination?.route == Screen.Playlists.route,
                        onClick = {
                            navController.navigate(Screen.Playlists.route) {
                                popUpTo(Screen.Library.route)
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Library.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Library.route) {
                    MusicLibraryScreen(
                        viewModel = musicViewModel,
                        onSongClick = { songId ->
                            val song = musicViewModel.getSongById(songId)
                            if (song != null) {
                                playerViewModel.playSong(song)
                                navController.navigate(Screen.Player.createRoute(songId))
                            }
                        },
                        onAddToPlaylist = { song ->
                            playlistViewModel.showAddToPlaylistDialog(song)
                        }
                    )
                }

                composable(Screen.Playlists.route) {
                    PlaylistScreen(
                        viewModel = playlistViewModel,
                        onPlaylistClick = { playlistId ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        viewModel = playlistViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onSongClick = { songId ->
                            val song = musicViewModel.getSongById(songId)
                            if (song != null) {
                                playerViewModel.playSong(song)
                                navController.navigate(Screen.Player.createRoute(songId))
                            }
                        },
                        onEditTimestamp = { playlistSongId, songId ->
                            navController.navigate(Screen.TimestampEditor.createRoute(playlistSongId, songId))
                        }
                    )
                }

                composable(
                    route = Screen.TimestampEditor.route,
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.LongType },
                        navArgument("songId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val playlistSongId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                    val songId = backStackEntry.arguments?.getLong("songId") ?: 0L

                    val song = musicViewModel.getSongById(songId)
                    val playlistSong = playlistViewModel.getPlaylistSongById(playlistSongId)

                    if (song != null) {
                        TimestampEditorScreen(
                            song = song,
                            initialStartTimestamp = playlistSong?.startTimestamp ?: 0,
                            initialEndTimestamp = playlistSong?.endTimestamp ?: -1,
                            onSave = { startTime, endTime ->
                                playlistViewModel.updateSongTimestamp(playlistSongId, startTime, endTime)
                            },
                            onBack = { navController.popBackStack() },
                            onPreview = { startTime, endTime ->
                                playerViewModel.previewTimestamp(song, startTime, endTime)
                            }
                        )
                    }
                }

                composable(
                    route = Screen.Player.route,
                    arguments = listOf(navArgument("songId") { type = NavType.LongType })
                ) {
                    PlayerScreen(
                        viewModel = playerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    } else {
        PermissionDeniedScreen(
            onRequestPermission = {
                permissionsState.launchMultiplePermissionRequest()
            }
        )
    }
}

@Composable
fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This app needs permission to access your music files.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}