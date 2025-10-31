package com.ashishkudale.musicapp.ui.screens.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: PlaylistViewModel,
    onNavigateBack: () -> Unit,
    onSongClick: (Long) -> Unit
) {
    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    val playlist by viewModel.currentPlaylist.collectAsState()
    val playlistSongsWithDetails by viewModel.playlistSongsWithDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var songs by remember { mutableStateOf(playlistSongsWithDetails) }

    LaunchedEffect(playlistSongsWithDetails) {
        songs = playlistSongsWithDetails
    }

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            songs = songs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { _, _ ->
            // Update positions in database
            val playlistSongs = songs.map { it.first }
            viewModel.reorderSongs(playlistSongs)
        }
    )

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
                songs.isEmpty() -> {
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
                        state = reorderableState.listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .reorderable(reorderableState)
                    ) {
                        itemsIndexed(songs, key = { _, item -> item.first.id }) { index, (playlistSong, song) ->
                            ReorderableItem(reorderableState, key = playlistSong.id) { isDragging ->
                                val elevation = if (isDragging) 8.dp else 0.dp

                                Surface(
                                    tonalElevation = elevation,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    PlaylistSongItem(
                                        playlistSong = playlistSong,
                                        song = song,
                                        position = index + 1,
                                        onSongClick = { song?.let { onSongClick(it.id) } },
                                        onRemove = {
                                            playlist?.let {
                                                viewModel.removeSongFromPlaylist(it.id, playlistSong.songId)
                                            }
                                        },
                                        onReorderHandle = reorderableState
                                    )
                                }
                            }
                            if (index < songs.size - 1) {
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
    onReorderHandle: ReorderableLazyListState
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag Handle
        IconButton(
            onClick = {},
            modifier = Modifier.detectReorderAfterLongPress(onReorderHandle)
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Position Number
        Text(
            text = "$position",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

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
                modifier = Modifier.padding(horizontal = 8.dp)
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
