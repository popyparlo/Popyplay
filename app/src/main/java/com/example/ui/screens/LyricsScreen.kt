package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncedLine
import com.example.data.model.Track
import com.example.ui.LyricsTab
import com.example.ui.components.WavySlider
import com.example.ui.theme.PixelDarkBg
import com.example.ui.theme.PixelMintSecondary
import com.example.ui.theme.PixelOnSecondary
import com.example.ui.theme.PixelRosePrimary

@Composable
fun LyricsScreen(
    currentTrack: Track?,
    syncedLines: List<SyncedLine>,
    selectedTab: LyricsTab,
    onTabSelect: (LyricsTab) -> Unit,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = currentTrack ?: return

    val listState = rememberLazyListState()

    // Find active synced line index
    val activeLineIndex by remember(currentPositionMs, syncedLines) {
        derivedStateOf {
            if (syncedLines.isEmpty()) -1
            else {
                val index = syncedLines.indexOfLast { it.timeMs <= currentPositionMs }
                if (index >= 0) index else 0
            }
        }
    }

    // Auto-scroll to active line
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && selectedTab == LyricsTab.SYNCED) {
            val target = (activeLineIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = modifier
            .testTag("lyrics_screen")
            .fillMaxSize()
            .background(PixelDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header: Back Arrow, "Lyrics" (Matching Screenshot 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .testTag("lyrics_back_button")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(48.dp)) // balance layout
            }

            // Synced / Static Pill Toggle (Matching Screenshot 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Synced Pill
                val isSynced = selectedTab == LyricsTab.SYNCED
                Surface(
                    modifier = Modifier
                        .testTag("lyrics_tab_synced")
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onTabSelect(LyricsTab.SYNCED) },
                    color = if (isSynced) PixelRosePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Synced",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSynced) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSynced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Static Pill
                val isStatic = selectedTab == LyricsTab.STATIC
                Surface(
                    modifier = Modifier
                        .testTag("lyrics_tab_static")
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onTabSelect(LyricsTab.STATIC) },
                    color = if (isStatic) PixelRosePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Static",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isStatic) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isStatic) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lyrics Content
            if (selectedTab == LyricsTab.SYNCED) {
                if (syncedLines.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .testTag("synced_lyrics_list")
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        itemsIndexed(syncedLines) { index, line ->
                            val isActive = index == activeLineIndex
                            val textColor by animateColorAsState(
                                targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                label = "lyrics_text_color"
                            )

                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = if (isActive) 23.sp else 20.sp,
                                    lineHeight = 32.sp
                                ),
                                color = textColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSeekTo(line.timeMs) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No synced lyrics available for this track.\nSwitch to Static or add an .lrc file.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Static lyrics
                Column(
                    modifier = Modifier
                        .testTag("static_lyrics_view")
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 180.dp)
                ) {
                    val staticText = track.staticLyrics ?: "No lyrics available for this track."
                    Text(
                        text = staticText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            lineHeight = 30.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Floating Bottom Playback Controller (Matching Screenshot 3)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Rounded Mint Green Play/Pause Pill
            Surface(
                modifier = Modifier
                    .testTag("lyrics_play_pause_button")
                    .size(width = 84.dp, height = 70.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onTogglePlayPause() },
                color = PixelMintSecondary,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = PixelOnSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Scrubber Pill (Matching Screenshot 3)
            val progress = if (durationMs > 0) {
                (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp)),
                color = Color(0xFF140D14),
                shape = RoundedCornerShape(27.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        WavySlider(
                            value = progress,
                            onValueChange = { frac ->
                                onSeekTo((frac * durationMs).toLong())
                            },
                            isPlaying = isPlaying,
                            activeColor = PixelRosePrimary,
                            inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            thumbColor = PixelRosePrimary
                        )
                    }

                    Text(
                        text = formatTime(durationMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%02d:%02d".format(minutes, seconds)
}
