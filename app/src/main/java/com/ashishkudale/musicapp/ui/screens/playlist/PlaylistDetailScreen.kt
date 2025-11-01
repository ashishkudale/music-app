package com.ashishkudale.musicapp.ui.screens.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ashishkudale.musicapp.data.database.entities.PlaylistSong
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: PlaylistViewModel,
    onNavigateBack: () -> Unit,
    onSongClick: (Long) -> Unit,
    onEditTimestamp: (Long, Long) -> Unit = { _, _ -> }
) {
    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    val playlist by viewModel.currentPlaylist.collectAsState()
    val playlistSongsWithDetails by viewModel.playlistSongsWithDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist?.name ?: "Playlist",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    playlist?.let {
                        IconButton(
                            onClick = { /* TODO: Play all songs in playlist */ }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play all")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                playlistSongsWithDetails.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No songs in this playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add songs from the library",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlistSongsWithDetails, key = { it.first.id }) { (playlistSong, song) ->
                            val position = playlistSongsWithDetails.indexOf(Pair(playlistSong, song)) + 1

                            PlaylistSongItem(
                                playlistSong = playlistSong,
                                song = song,
                                position = position,
                                onSongClick = { song?.let { onSongClick(it.id) } },
                                onRemove = {
                                    playlist?.let {
                                        viewModel.removeSongFromPlaylist(it.id, playlistSong.songId)
                                    }
                                },
                                onEditTimestamp = {
                                    song?.let { onEditTimestamp(playlistSong.id, it.id) }
                                }
                            )

                            if (position < playlistSongsWithDetails.size) {
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistSongItem(
    playlistSong: PlaylistSong,
    song: Song?,
    position: Int,
    onSongClick: () -> Unit,
    onRemove: () -> Unit,
    onEditTimestamp: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position Number
        Text(
            text = "$position",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Album Art or Music Icon
        if (song?.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album art",
                modifier = Modifier.size(48.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Music",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song?.title ?: "Unknown Song",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Show timestamp info if custom range is set
            if (playlistSong.startTimestamp > 0 || playlistSong.endTimestamp != -1L) {
                val startTime = TimeFormatter.formatTime(playlistSong.startTimestamp)
                val endTime = if (playlistSong.endTimestamp == -1L) {
                    "End"
                } else {
                    TimeFormatter.formatTime(playlistSong.endTimestamp)
                }
                Text(
                    text = "Range: $startTime - $endTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Duration
        song?.let {
            Text(
                text = TimeFormatter.formatTime(it.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Edit Timestamp Button
        IconButton(onClick = onEditTimestamp) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit timestamp",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Remove Button
        IconButton(onClick = { showRemoveDialog = true }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove from playlist",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    // Remove Confirmation Dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Song") },
            text = { Text("Remove \"${song?.title}\" from this playlist?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
