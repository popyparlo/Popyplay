package com.example.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Track
import com.example.ui.theme.PixelDarkBg
import com.example.ui.theme.PixelPeriwinkle
import com.example.ui.theme.PixelRoseContainer
import com.example.ui.theme.PixelRosePrimary

@Composable
fun HomeScreen(
    tracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .testTag("home_screen")
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PixelDarkBg,
                        Color(0xFF141926),
                        PixelDarkBg
                    )
                )
            ),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top action bar (Calendar/Mix icon and Settings)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier
                        .testTag("home_calendar_action")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Queue or History",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .testTag("home_settings_action")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Hero Headline & Play Mix Button (Matching Screenshot 1)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your\nMix",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 46.sp,
                            lineHeight = 48.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Today's Mix for you",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Big circular Play Button
                Surface(
                    modifier = Modifier
                        .testTag("hero_play_mix_button")
                        .size(88.dp)
                        .clip(CircleShape)
                        .clickable { onPlayAll(tracks, true) },
                    color = PixelPeriwinkle,
                    shape = CircleShape,
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Mix",
                            tint = Color(0xFF0F1E36),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        // Dynamic Album Art Collage (Matching Screenshot 1)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Secondary small left artwork circle
                val leftArt = tracks.getOrNull(1)?.albumArtUri
                Surface(
                    modifier = Modifier
                        .offset(x = (-120).dp, y = (-20).dp)
                        .size(76.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    shadowElevation = 4.dp
                ) {
                    AsyncImage(
                        model = leftArt,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Main center oval collage piece (rotated and styled like Screenshot 1)
                val centerArt = tracks.getOrNull(0)?.albumArtUri
                Surface(
                    modifier = Modifier
                        .size(width = 240.dp, height = 180.dp)
                        .rotate(-32f)
                        .clip(RoundedCornerShape(90.dp))
                        .clickable {
                            if (tracks.isNotEmpty()) onPlayTrack(tracks[0])
                        },
                    color = PixelRoseContainer,
                    shape = RoundedCornerShape(90.dp),
                    shadowElevation = 10.dp
                ) {
                    AsyncImage(
                        model = centerArt,
                        contentDescription = "Featured Mix Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(32f) // Unrotate inside content for upright look
                    )
                }

                // Secondary bottom right artwork circle
                val rightArt = tracks.getOrNull(2)?.albumArtUri
                Surface(
                    modifier = Modifier
                        .offset(x = 110.dp, y = 80.dp)
                        .size(80.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    shadowElevation = 4.dp
                ) {
                    AsyncImage(
                        model = rightArt,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Quick Picks Section
        item {
            Text(
                text = "Quick Picks",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(tracks.take(6)) { track ->
                    Card(
                        modifier = Modifier
                            .testTag("quick_pick_card_${track.id}")
                            .width(160.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onPlayTrack(track) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PixelRoseContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!track.albumArtUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = track.albumArtUri,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = PixelRosePrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
