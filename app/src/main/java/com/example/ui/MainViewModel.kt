package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.LyricsParser
import com.example.data.model.Playlist
import com.example.data.model.SyncedLine
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import com.example.player.PixelAudioPlayer
import com.example.player.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    HOME,
    SEARCH,
    LIBRARY
}

enum class LibraryTab {
    SONGS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS
}

enum class LyricsTab {
    SYNCED,
    STATIC
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val player = PixelAudioPlayer(application)

    // Player State Flows
    val currentTrack: StateFlow<Track?> = player.currentTrack
    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val currentPositionMs: StateFlow<Long> = player.currentPositionMs
    val durationMs: StateFlow<Long> = player.durationMs
    val repeatMode: StateFlow<RepeatMode> = player.repeatMode
    val isShuffle: StateFlow<Boolean> = player.isShuffle
    val queue: StateFlow<List<Track>> = player.queue

    // UI Navigation State
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _libraryTab = MutableStateFlow(LibraryTab.SONGS)
    val libraryTab: StateFlow<LibraryTab> = _libraryTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _isLyricsExpanded = MutableStateFlow(false)
    val isLyricsExpanded: StateFlow<Boolean> = _isLyricsExpanded.asStateFlow()

    private val _lyricsTab = MutableStateFlow(LyricsTab.SYNCED)
    val lyricsTab: StateFlow<LyricsTab> = _lyricsTab.asStateFlow()

    // Library & Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks: StateFlow<List<Track>> = _allTracks.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Track>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<Track>> = combine(_allTracks, _searchQuery) { tracks, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            tracks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Synced Lyrics parsed for current track
    val parsedSyncedLyrics: StateFlow<List<SyncedLine>> = combine(currentTrack) { tracks ->
        val track = tracks[0]
        LyricsParser.parseLrc(track?.lrcLyrics)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback message banner for user notifications
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            val tracks = repository.queryDeviceAudio()
            _allTracks.value = tracks
            if (player.currentTrack.value == null && tracks.isNotEmpty()) {
                // Initialize queue with tracks without auto-playing immediately
                player.setQueue(tracks, startIndex = 0, autoPlay = false)
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setLibraryTab(tab: LibraryTab) {
        _libraryTab.value = tab
    }

    fun setLyricsTab(tab: LyricsTab) {
        _lyricsTab.value = tab
    }

    fun openNowPlaying() {
        _isNowPlayingExpanded.value = true
        _isLyricsExpanded.value = false
    }

    fun closeNowPlaying() {
        _isNowPlayingExpanded.value = false
    }

    fun openLyrics() {
        _isLyricsExpanded.value = true
    }

    fun closeLyrics() {
        _isLyricsExpanded.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        val activeQueue = newQueue ?: _allTracks.value
        val index = activeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        player.setQueue(activeQueue, startIndex = index, autoPlay = true)
    }

    fun playAll(tracks: List<Track>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        if (shuffle) {
            if (!player.isShuffle.value) player.toggleShuffle()
            player.setQueue(tracks.shuffled(), startIndex = 0, autoPlay = true)
        } else {
            player.setQueue(tracks, startIndex = 0, autoPlay = true)
        }
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun skipNext() {
        player.skipNext()
    }

    fun skipPrevious() {
        player.skipPrevious()
    }

    fun toggleShuffle() {
        player.toggleShuffle()
    }

    fun cycleRepeatMode() {
        player.cycleRepeatMode()
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
            // Update local track state
            _allTracks.value = _allTracks.value.map {
                if (it.contentUri == track.contentUri) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }

    /**
     * Feature: "Use individual files as playlists"
     * Adds an individual file selected via SAF as a dedicated standalone playlist
     */
    fun createPlaylistFromSingleFile(uri: Uri, customName: String? = null) {
        viewModelScope.launch {
            try {
                val playlist = repository.createPlaylistFromIndividualFile(uri, customName)
                _toastMessage.value = "Created single-file playlist: ${playlist.name}"
                // Switch to playlists tab to see it immediately
                _currentTab.value = AppTab.LIBRARY
                _libraryTab.value = LibraryTab.PLAYLISTS
            } catch (e: Exception) {
                _toastMessage.value = "Failed to create playlist from file: ${e.message}"
            }
        }
    }

    /**
     * Feature: Import .m3u playlist file
     */
    fun importM3uPlaylist(uri: Uri, customName: String? = null) {
        viewModelScope.launch {
            try {
                val playlist = repository.importM3uPlaylist(uri, customName)
                _toastMessage.value = "Imported playlist: ${playlist.name} (${playlist.trackCount} tracks)"
                _currentTab.value = AppTab.LIBRARY
                _libraryTab.value = LibraryTab.PLAYLISTS
            } catch (e: Exception) {
                _toastMessage.value = "Failed to import playlist: ${e.message}"
            }
        }
    }

    fun createCustomPlaylist(name: String) {
        viewModelScope.launch {
            repository.createCustomPlaylist(name)
            _toastMessage.value = "Created playlist '$name'"
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _toastMessage.value = "Playlist deleted"
        }
    }

    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.getTracksForPlaylist(playlist.id).collect { tracks ->
                if (tracks.isNotEmpty()) {
                    player.setQueue(tracks, startIndex = 0, autoPlay = true)
                    // If it's a single file playlist, automatically set repeat ONE for seamless looping!
                    if (playlist.type == com.example.data.model.PlaylistType.SINGLE_FILE) {
                        if (player.repeatMode.value != RepeatMode.ONE) {
                            player.cycleRepeatMode()
                        }
                    }
                    _toastMessage.value = "Playing ${playlist.name}"
                } else {
                    _toastMessage.value = "Playlist is empty"
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
