package com.ashishkudale.musicapp.ui.screens.timestamp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ashishkudale.musicapp.data.model.Song
import com.ashishkudale.musicapp.util.TimeFormatter
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimestampEditorScreen(
    song: Song,
    initialStartTimestamp: Long = 0,
    initialEndTimestamp: Long = -1,
    onSave: (Long, Long) -> Unit,
    onBack: () -> Unit,
    onPreview: (Long, Long) -> Unit
) {
    var startTimestamp by remember { mutableStateOf(initialStartTimestamp) }
    var endTimestamp by remember {
        mutableStateOf(if (initialEndTimestamp == -1L) song.duration else initialEndTimestamp)
    }
    var playTillEnd by remember { mutableStateOf(initialEndTimestamp == -1L) }

    // Ensure start is before end
    if (startTimestamp >= endTimestamp && !playTillEnd) {
        startTimestamp = 0
        endTimestamp = song.duration
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Timestamp") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val finalEndTimestamp = if (playTillEnd) -1L else endTimestamp
                            onSave(startTimestamp, finalEndTimestamp)
                            onBack()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Song Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (song.albumArtUri != null) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = "Album art",
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Music",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${TimeFormatter.formatTime(song.duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Timeline visualization
            TimelineVisualization(
                totalDuration = song.duration,
                startTime = startTimestamp,
                endTime = if (playTillEnd) song.duration else endTimestamp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Start Timestamp Selector
            TimestampSelector(
                label = "Start Time",
                value = startTimestamp,
                maxValue = song.duration,
                onValueChange = {
                    startTimestamp = it
                    // Ensure start doesn't exceed end
                    if (!playTillEnd && startTimestamp >= endTimestamp) {
                        endTimestamp = (startTimestamp + 1000).coerceAtMost(song.duration)
                    }
                },
                icon = Icons.Default.PlayArrow,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Play till end toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Play till end",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Switch(
                        checked = playTillEnd,
                        onCheckedChange = {
                            playTillEnd = it
                            if (!it && endTimestamp == song.duration) {
                                endTimestamp = (song.duration * 0.9).roundToLong()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // End Timestamp Selector (disabled if play till end is on)
            TimestampSelector(
                label = "End Time",
                value = endTimestamp,
                maxValue = song.duration,
                onValueChange = {
                    endTimestamp = it
                    // Ensure end doesn't go before start
                    if (endTimestamp <= startTimestamp) {
                        startTimestamp = (endTimestamp - 1000).coerceAtLeast(0)
                    }
                },
                icon = Icons.Default.Stop,
                color = MaterialTheme.colorScheme.error,
                enabled = !playTillEnd
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Segment duration display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Segment Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val segmentDuration = if (playTillEnd) {
                        song.duration - startTimestamp
                    } else {
                        endTimestamp - startTimestamp
                    }
                    Text(
                        text = TimeFormatter.formatTime(segmentDuration),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Preview button
            Button(
                onClick = {
                    onPreview(
                        startTimestamp,
                        if (playTillEnd) -1L else endTimestamp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview Segment")
            }
        }
    }
}

@Composable
fun TimestampSelector(
    label: String,
    value: Long,
    maxValue: Long,
    onValueChange: (Long) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                Text(
                    text = TimeFormatter.formatTime(value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToLong()) },
                valueRange = 0f..maxValue.toFloat(),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color
                )
            )
        }
    }
}

@Composable
fun TimelineVisualization(
    totalDuration: Long,
    startTime: Long,
    endTime: Long,
    modifier: Modifier = Modifier
) {
    val startPercent = (startTime.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val endPercent = (endTime.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        // Timeline bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            // Background (unselected area)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
            )

            // Before start (grayed out)
            if (startPercent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(startPercent)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }

            // Selected range
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(endPercent - startPercent)
                    .offset(x = (startPercent * 100).dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    )
            )

            // After end (grayed out)
            if (endPercent < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f - endPercent)
                        .align(Alignment.CenterEnd)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0:00",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = TimeFormatter.formatTime(totalDuration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
