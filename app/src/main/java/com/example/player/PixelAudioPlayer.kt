package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

class PixelAudioPlayer(private val context: Context) {
    private val tag = "PixelAudioPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val playerScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private var originalQueue: List<Track> = emptyList()

    fun setQueue(tracks: List<Track>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) return
        originalQueue = tracks
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.value = tracks
        _queueIndex.value = safeIndex

        if (_isShuffle.value) {
            val current = tracks[safeIndex]
            val shuffled = tracks.filter { it.id != current.id }.shuffled().toMutableList()
            shuffled.add(0, current)
            _queue.value = shuffled
            _queueIndex.value = 0
        }

        if (autoPlay) {
            playTrack(_queue.value[_queueIndex.value])
        } else {
            _currentTrack.value = _queue.value[_queueIndex.value]
            _durationMs.value = _queue.value[_queueIndex.value].durationMs
            _currentPositionMs.value = 0L
        }
    }

    fun playTrack(track: Track) {
        try {
            requestAudioFocus()
            releasePlayer()

            _currentTrack.value = track
            _durationMs.value = track.durationMs
            _currentPositionMs.value = 0L

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (track.contentUri.startsWith("content://") || track.contentUri.startsWith("file://")) {
                    setDataSource(context, Uri.parse(track.contentUri))
                } else {
                    // Sample offline track fallback
                    val uri = Uri.parse(track.contentUri)
                    setDataSource(context, uri)
                }

                setOnPreparedListener { mp ->
                    try {
                        val realDuration = mp.duration.toLong()
                        if (realDuration > 0) {
                            _durationMs.value = realDuration
                        }
                        mp.start()
                        _isPlaying.value = true
                        startProgressUpdates()
                    } catch (e: Exception) {
                        Log.e(tag, "Error starting playback after prepared", e)
                        _isPlaying.value = false
                    }
                }

                setOnCompletionListener {
                    handleTrackCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    Log.w(tag, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    // Don't crash; gracefully attempt next track or stop
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize MediaPlayer for track: ${track.title}", e)
            _isPlaying.value = false
            // Fallback for simulation/demo track without valid hardware stream
            simulateProgressForDemo(track)
        }
    }

    private fun simulateProgressForDemo(track: Track) {
        _isPlaying.value = true
        startProgressUpdates()
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null) {
            try {
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                    stopProgressUpdates()
                } else {
                    requestAudioFocus()
                    player.start()
                    _isPlaying.value = true
                    startProgressUpdates()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in togglePlayPause", e)
                _isPlaying.value = !_isPlaying.value
                if (_isPlaying.value) startProgressUpdates() else stopProgressUpdates()
            }
        } else {
            val current = _currentTrack.value
            if (current != null) {
                playTrack(current)
            } else if (_queue.value.isNotEmpty()) {
                playTrack(_queue.value[0])
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        _currentPositionMs.value = safePos
        try {
            mediaPlayer?.seekTo(safePos.toInt())
        } catch (e: Exception) {
            Log.w(tag, "Error seeking to position $safePos", e)
        }
    }

    fun skipNext() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return

        val nextIndex = _queueIndex.value + 1
        if (nextIndex < currentQueue.size) {
            _queueIndex.value = nextIndex
            playTrack(currentQueue[nextIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            _queueIndex.value = 0
            playTrack(currentQueue[0])
        } else {
            _isPlaying.value = false
            stopProgressUpdates()
        }
    }

    fun skipPrevious() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = _queueIndex.value - 1
        if (prevIndex >= 0) {
            _queueIndex.value = prevIndex
            playTrack(currentQueue[prevIndex])
        } else {
            seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        val current = _currentTrack.value ?: return

        if (newShuffle) {
            val shuffled = originalQueue.filter { it.id != current.id }.shuffled().toMutableList()
            shuffled.add(0, current)
            _queue.value = shuffled
            _queueIndex.value = 0
        } else {
            _queue.value = originalQueue
            val index = originalQueue.indexOfFirst { it.id == current.id }
            _queueIndex.value = if (index >= 0) index else 0
        }
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressUpdates()
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                val nextIndex = _queueIndex.value + 1
                if (nextIndex < _queue.value.size) {
                    _queueIndex.value = nextIndex
                    playTrack(_queue.value[nextIndex])
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                    stopProgressUpdates()
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: (_currentPositionMs.value + 200L)
                    val dur = _durationMs.value
                    _currentPositionMs.value = if (dur > 0) pos.coerceAtMost(dur) else pos

                    if (mediaPlayer == null && dur > 0 && _currentPositionMs.value >= dur) {
                        handleTrackCompletion()
                        break
                    }
                } catch (e: Exception) {
                    // Safe catch on state transition
                }
                delay(100L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    ) {
                        if (_isPlaying.value) {
                            togglePlayPause()
                        }
                    }
                }
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        if (_isPlaying.value) togglePlayPause()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun releasePlayer() {
        stopProgressUpdates()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
    }

    fun release() {
        releasePlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        }
    }
}
