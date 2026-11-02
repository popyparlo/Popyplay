package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "CUSTOM", "SINGLE_FILE", "IMPORTED_FILE"
    val fileUri: String? = null,
    val filePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "trackContentUri"]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val trackContentUri: String,
    val trackTitle: String,
    val trackArtist: String,
    val trackAlbum: String,
    val trackDurationMs: Long,
    val albumArtUri: String? = null,
    val orderIndex: Int = 0
)

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtUri: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
