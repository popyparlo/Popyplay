package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.PixelBlueAccent
import com.example.ui.theme.PixelDarkBg
import com.example.ui.theme.PixelPlayerTheme
import com.example.ui.theme.PixelRosePrimary

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelPlayerTheme {
                PixelPlayerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PixelPlayerApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State collections
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val libraryTab by viewModel.libraryTab.collectAsStateWithLifecycle()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val isLyricsExpanded by viewModel.isLyricsExpanded.collectAsStateWithLifecycle()
    val lyricsTab by viewModel.lyricsTab.collectAsStateWithLifecycle()

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()

    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val syncedLyrics by viewModel.parsedSyncedLyrics.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }

    // Android 10 (API 29) - Android 15 Runtime Storage Permission Setup
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadLibrary()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            permissionToRequest
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(permissionToRequest)
        }
    }

    // Toast message handling
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastMessage()
        }
    }

    // System BackHandler
    BackHandler(enabled = isLyricsExpanded || isNowPlayingExpanded || currentTab != AppTab.HOME) {
        when {
            isLyricsExpanded -> viewModel.closeLyrics()
            isNowPlayingExpanded -> viewModel.closeNowPlaying()
            currentTab != AppTab.HOME -> viewModel.setTab(AppTab.HOME)
        }
    }

    val miniPlayerProgress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Persistent Bottom Bar containing MiniPlayer + NavigationBar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PixelDarkBg)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Docked MiniPlayer right above bottom navigation bar
                if (currentTrack != null && !isNowPlayingExpanded && !isLyricsExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        MiniPlayer(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            progress = miniPlayerProgress,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onOpenNowPlaying = { viewModel.openNowPlaying() }
                        )
                    }
                }

                // Pixel-styled Navigation Bar (Home, Search, Library) Matching Screenshots 1 & 4
                NavigationBar(
                    modifier = Modifier
                        .testTag("main_bottom_nav")
                        .fillMaxWidth(),
                    containerColor = PixelDarkBg,
                    tonalElevation = 0.dp
                ) {
                    // Home
                    NavigationBarItem(
                        selected = currentTab == AppTab.HOME,
                        onClick = { viewModel.setTab(AppTab.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PixelDarkBg,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = PixelRosePrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Search
                    NavigationBarItem(
                        selected = currentTab == AppTab.SEARCH,
                        onClick = { viewModel.setTab(AppTab.SEARCH) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Search", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PixelDarkBg,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = PixelRosePrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Library
                    NavigationBarItem(
                        selected = currentTab == AppTab.LIBRARY,
                        onClick = { viewModel.setTab(AppTab.LIBRARY) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.LIBRARY) Icons.Filled.QueueMusic else Icons.Outlined.QueueMusic,
                                contentDescription = "Library"
                            )
                        },
                        label = { Text("Library", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0C1322),
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = PixelBlueAccent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Content Tabs
            when (currentTab) {
                AppTab.HOME -> {
                    HomeScreen(
                        tracks = allTracks,
                        onPlayTrack = { track -> viewModel.playTrack(track) },
                        onPlayAll = { tracks, shuffle -> viewModel.playAll(tracks, shuffle) },
                        onOpenQueue = {
                            viewModel.setTab(AppTab.LIBRARY)
                            viewModel.setLibraryTab(com.example.ui.LibraryTab.SONGS)
                        },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
                AppTab.SEARCH -> {
                    SearchScreen(
                        searchQuery = searchQuery,
                        onQueryChange = { q -> viewModel.updateSearchQuery(q) },
                        searchResults = searchResults,
                        allTracks = allTracks,
                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) }
                    )
                }
                AppTab.LIBRARY -> {
                    LibraryScreen(
                        tracks = allTracks,
                        playlists = playlists,
                        selectedLibraryTab = libraryTab,
                        onSelectLibraryTab = { tab -> viewModel.setLibraryTab(tab) },
                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                        onPlayAll = { tracks, shuffle -> viewModel.playAll(tracks, shuffle) },
                        onPlayPlaylist = { pl -> viewModel.playPlaylist(pl) },
                        onCreatePlaylistFromSingleFile = { uri -> viewModel.createPlaylistFromSingleFile(uri) },
                        onImportM3uPlaylist = { uri -> viewModel.importM3uPlaylist(uri) },
                        onCreateCustomPlaylist = { name -> viewModel.createCustomPlaylist(name) },
                        onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
            }

            // Full-screen Now Playing overlay with smooth slide transition
            AnimatedVisibility(
                visible = isNowPlayingExpanded && !isLyricsExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NowPlayingScreen(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { pos -> viewModel.seekTo(pos) },
                    onSkipNext = { viewModel.skipNext() },
                    onSkipPrevious = { viewModel.skipPrevious() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                    onOpenLyrics = { viewModel.openLyrics() },
                    onClose = { viewModel.closeNowPlaying() }
                )
            }

            // Full-screen Lyrics overlay with smooth slide transition
            AnimatedVisibility(
                visible = isLyricsExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                LyricsScreen(
                    currentTrack = currentTrack,
                    syncedLines = syncedLyrics,
                    selectedTab = lyricsTab,
                    onTabSelect = { tab -> viewModel.setLyricsTab(tab) },
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { pos -> viewModel.seekTo(pos) },
                    onClose = { viewModel.closeLyrics() }
                )
            }
        }
    }

    // Settings & Compatibility Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "PixelPlayer Settings",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Android 10 Compatibility",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PixelRosePrimary
                    )
                    Text(
                        text = "• Target SDK: 36 (Android 15+)\n• Min SDK: 24 (Compatible with Android 10 / API 29)\n• Scoped Storage Mode: Legacy fallback active\n• Native MediaPlayer hardware decoding enabled for low-latency & crash prevention on older chipsets.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Text(
                        text = "Individual Files as Playlists",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PixelBlueAccent
                    )
                    Text(
                        text = "• Open Library -> PLAYLISTS -> 'Use File as Playlist'\n• Pick any audio file (.mp3, .m4a, .flac, .wav)\n• Automatic single-track loop playback enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}
