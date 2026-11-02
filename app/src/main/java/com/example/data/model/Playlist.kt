package com.example.data.model

enum class PlaylistType {
    CUSTOM,
    SINGLE_FILE, // Direct individual audio file used as a dedicated looping/standalone playlist
    IMPORTED_FILE // Imported .m3u / .m3u8 playlist file
}

data class Playlist(
    val id: Long = 0,
    val name: String,
    val type: PlaylistType = PlaylistType.CUSTOM,
    val fileUri: String? = null,
    val filePath: String? = null,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
