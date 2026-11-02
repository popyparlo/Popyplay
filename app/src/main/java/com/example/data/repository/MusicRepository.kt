package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteTrackEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistItemEntity
import com.example.data.model.Playlist
import com.example.data.model.PlaylistType
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {
    private val tag = "MusicRepository"
    private val db = AppDatabase.getDatabase(context)
    private val playlistDao = db.playlistDao()
    private val favoriteDao = db.favoriteDao()

    // Curated high-fidelity starter catalog matching Screenshots
    val sampleTracks = listOf(
        Track(
            id = -1L,
            title = "The Emptiness Machine",
            artist = "Linkin Park",
            album = "From Zero",
            durationMs = 190_000L, // 3:10
            contentUri = "android.resource://${context.packageName}/raw/the_emptiness_machine",
            albumArtUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=80",
            isSample = true,
            lrcLyrics = """
                [00:00.00] Already under my skin
                [00:04.50] And I know exactly how this ends, I
                [00:09.20] Let you cut me open
                [00:13.80] Just to watch me bleed
                [00:18.40] Gave up who I am for who you wanted me to be
                [00:23.00] Don't know why I'm hoping
                [00:27.60] For what I won't receive
                [00:32.20] Falling for the promise of
                [00:36.80] The emptiness machine
                [00:41.40] I only wanted to be part of something
                [00:46.00] I only wanted to be part of something
                [00:50.60] Going around in circles
                [00:55.20] Till there's nothing left to lose
                [01:00.00] Now I know exactly who you are
            """.trimIndent(),
            staticLyrics = """
                Already under my skin
                And I know exactly how this ends, I
                Let you cut me open
                Just to watch me bleed
                Gave up who I am for who you wanted me to be
                Don't know why I'm hoping
                For what I won't receive
                Falling for the promise of
                The emptiness machine
                I only wanted to be part of something
                I only wanted to be part of something
                Going around in circles
                Till there's nothing left to lose
                Now I know exactly who you are
            """.trimIndent()
        ),
        Track(
            id = -2L,
            title = "1x1 (feat. Nova Twins)",
            artist = "Bring Me The Horizon",
            album = "POST HUMAN: SURVIVAL HORROR",
            durationMs = 209_000L, // 3:29
            contentUri = "android.resource://${context.packageName}/raw/one_by_one",
            albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80",
            isSample = true,
            lrcLyrics = """
                [00:00.00] Put me out of my misery
                [00:05.00] My mind feels like an enemy, yeah
                [00:10.00] Another day, another war
                [00:15.00] One by one, taking me down
                [00:20.00] No way out, nowhere to run
            """.trimIndent(),
            staticLyrics = """
                Put me out of my misery
                My mind feels like an enemy, yeah
                Another day, another war
                One by one, taking me down
                No way out, nowhere to run
            """.trimIndent()
        ),
        Track(
            id = -3L,
            title = "Be Yourself",
            artist = "Audioslave",
            album = "Out of Exile",
            durationMs = 279_000L, // 4:39
            contentUri = "android.resource://${context.packageName}/raw/be_yourself",
            albumArtUri = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?w=500&auto=format&fit=crop&q=80",
            isSample = true,
            lrcLyrics = """
                [00:00.00] Someone falling into water
                [00:06.00] Can't be hurt by downpour
                [00:12.00] And be yourself is all that you can do
                [00:18.00] To be yourself is all that you can do
            """.trimIndent(),
            staticLyrics = """
                Someone falling into water
                Can't be hurt by downpour
                And be yourself is all that you can do
                To be yourself is all that you can do
            """.trimIndent()
        ),
        Track(
            id = -4L,
            title = "Big Me",
            artist = "Foo Fighters",
            album = "Foo Fighters",
            durationMs = 132_000L, // 2:12
            contentUri = "android.resource://${context.packageName}/raw/big_me",
            albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80",
            isSample = true,
            lrcLyrics = """
                [00:00.00] When they convince you to hold out
                [00:05.00] Keep it inside yourself
                [00:10.00] Say when, say when
            """.trimIndent(),
            staticLyrics = """
                When they convince you to hold out
                Keep it inside yourself
                Say when, say when
            """.trimIndent()
        ),
        Track(
            id = -5L,
            title = "Breaking the Habit",
            artist = "Linkin Park",
            album = "Meteora",
            durationMs = 196_000L, // 3:16
            contentUri = "android.resource://${context.packageName}/raw/breaking_the_habit",
            albumArtUri = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500&auto=format&fit=crop&q=80",
            isSample = true,
            lrcLyrics = """
                [00:00.00] Memories consume
                [00:04.00] Like opening the wound
                [00:08.00] I'm picking me apart again
                [00:12.00] You all assume
                [00:15.00] I'm safe here in my room
                [00:19.00] Unless I try to begin
            """.trimIndent(),
            staticLyrics = """
                Memories consume
                Like opening the wound
                I'm picking me apart again
                You all assume
                I'm safe here in my room
                Unless I try to begin
            """.trimIndent()
        )
    )

    /**
     * Safe query for local MediaStore audio that never crashes on Android 10 or older ROMs.
     * Guards against missing columns, null cursors, and Permission issues.
     */
    suspend fun queryDeviceAudio(): List<Track> = withContext(Dispatchers.IO) {
        val deviceTracks = mutableListOf<Track>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // Defensive column projection
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val id = if (idCol >= 0) c.getLong(idCol) else continue
                    val title = if (titleCol >= 0) c.getString(titleCol) ?: "Unknown Track" else "Unknown Track"
                    val artist = if (artistCol >= 0) c.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist"
                    val album = if (albumCol >= 0) c.getString(albumCol) ?: "Unknown Album" else "Unknown Album"
                    val duration = if (durationCol >= 0) c.getLong(durationCol) else 0L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

                    val albumArtUri = if (albumIdCol >= 0) {
                        val albumId = c.getLong(albumIdCol)
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else null

                    deviceTracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            durationMs = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            isSample = false
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w(tag, "Storage permission not yet granted or denied for MediaStore: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error reading MediaStore on device", e)
        }

        // Return combined device tracks + sample tracks
        if (deviceTracks.isEmpty()) {
            sampleTracks
        } else {
            deviceTracks + sampleTracks
        }
    }

    /**
     * Feature: "Use individual files as playlists"
     * Allows selecting an individual audio file directly from device storage (SAF)
     * and turning it into a dedicated single-track playlist (with auto-loop support)
     */
    suspend fun createPlaylistFromIndividualFile(fileUri: Uri, customName: String? = null): Playlist = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName = customName

        // Extract filename from ContentResolver if name not provided
        if (displayName.isNullOrBlank()) {
            resolver.query(fileUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    val raw = cursor.getString(nameIndex)
                    displayName = raw?.substringBeforeLast('.') ?: "Audio File Playlist"
                }
            }
        }

        // Extract metadata using MediaMetadataRetriever defensively
        var title = displayName ?: "Track"
        var artist = "Unknown Artist"
        var album = "Single File Playlist"
        var durationMs = 0L

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, fileUri)
            val extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val extractedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val extractedAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val extractedDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

            if (!extractedTitle.isNullOrBlank()) title = extractedTitle
            if (!extractedArtist.isNullOrBlank()) artist = extractedArtist
            if (!extractedAlbum.isNullOrBlank()) album = extractedAlbum
            if (extractedDuration != null && extractedDuration > 0) durationMs = extractedDuration

            retriever.release()
        } catch (e: Exception) {
            Log.w(tag, "Could not extract metadata via retriever for file: $fileUri", e)
        }

        val playlistName = displayName ?: title

        // Insert playlist in DB
        val playlistEntity = PlaylistEntity(
            name = playlistName,
            type = PlaylistType.SINGLE_FILE.name,
            fileUri = fileUri.toString()
        )
        val playlistId = playlistDao.insertPlaylist(playlistEntity)

        // Insert item in playlist
        playlistDao.insertItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                trackContentUri = fileUri.toString(),
                trackTitle = title,
                trackArtist = artist,
                trackAlbum = album,
                trackDurationMs = durationMs,
                albumArtUri = null,
                orderIndex = 0
            )
        )

        Playlist(
            id = playlistId,
            name = playlistName,
            type = PlaylistType.SINGLE_FILE,
            fileUri = fileUri.toString(),
            trackCount = 1
        )
    }

    /**
     * Feature: Import .m3u / .m3u8 playlist file
     */
    suspend fun importM3uPlaylist(fileUri: Uri, customName: String? = null): Playlist = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name = customName ?: "Imported Playlist"

        resolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val raw = cursor.getString(nameIndex)
                if (!raw.isNullOrBlank()) {
                    name = raw.substringBeforeLast('.')
                }
            }
        }

        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name,
                type = PlaylistType.IMPORTED_FILE.name,
                fileUri = fileUri.toString()
            )
        )

        // Read M3U lines
        val items = mutableListOf<PlaylistItemEntity>()
        try {
            resolver.openInputStream(fileUri)?.bufferedReader()?.useLines { lines ->
                var currentTitle = ""
                var currentArtist = ""
                var order = 0

                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXTINF:")) {
                        val info = trimmed.substringAfter("#EXTINF:")
                        val parts = info.substringAfter(',').split(" - ")
                        if (parts.size >= 2) {
                            currentArtist = parts[0].trim()
                            currentTitle = parts[1].trim()
                        } else {
                            currentTitle = info.substringAfter(',').trim()
                        }
                    } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        items.add(
                            PlaylistItemEntity(
                                playlistId = playlistId,
                                trackContentUri = trimmed,
                                trackTitle = if (currentTitle.isNotEmpty()) currentTitle else trimmed.substringAfterLast('/'),
                                trackArtist = if (currentArtist.isNotEmpty()) currentArtist else "Unknown Artist",
                                trackAlbum = name,
                                trackDurationMs = 180_000L,
                                orderIndex = order++
                            )
                        )
                        currentTitle = ""
                        currentArtist = ""
                    }
                }
            }
            if (items.isNotEmpty()) {
                playlistDao.insertItems(items)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reading M3U file", e)
        }

        Playlist(
            id = playlistId,
            name = name,
            type = PlaylistType.IMPORTED_FILE,
            fileUri = fileUri.toString(),
            trackCount = items.size
        )
    }

    suspend fun createCustomPlaylist(name: String, tracks: List<Track> = emptyList()): Long = withContext(Dispatchers.IO) {
        val id = playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name,
                type = PlaylistType.CUSTOM.name
            )
        )
        if (tracks.isNotEmpty()) {
            val items = tracks.mapIndexed { index, track ->
                PlaylistItemEntity(
                    playlistId = id,
                    trackContentUri = track.contentUri,
                    trackTitle = track.title,
                    trackArtist = track.artist,
                    trackAlbum = track.album,
                    trackDurationMs = track.durationMs,
                    albumArtUri = track.albumArtUri,
                    orderIndex = index
                )
            }
            playlistDao.insertItems(items)
        }
        id
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = withContext(Dispatchers.IO) {
        val count = playlistDao.getTrackCount(playlistId)
        playlistDao.insertItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                trackContentUri = track.contentUri,
                trackTitle = track.title,
                trackArtist = track.artist,
                trackAlbum = track.album,
                trackDurationMs = track.durationMs,
                albumArtUri = track.albumArtUri,
                orderIndex = count
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.clearItemsForPlaylist(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                val count = playlistDao.getTrackCount(entity.id)
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    type = try {
                        PlaylistType.valueOf(entity.type)
                    } catch (e: Exception) {
                        PlaylistType.CUSTOM
                    },
                    fileUri = entity.fileUri,
                    filePath = entity.filePath,
                    trackCount = count,
                    createdAt = entity.createdAt
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getItemsForPlaylist(playlistId).map { items ->
            items.map { item ->
                Track(
                    id = item.trackContentUri.hashCode().toLong(),
                    title = item.trackTitle,
                    artist = item.trackArtist,
                    album = item.trackAlbum,
                    durationMs = item.trackDurationMs,
                    contentUri = item.trackContentUri,
                    albumArtUri = item.albumArtUri
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getFavorites(): Flow<List<Track>> {
        return favoriteDao.getAllFavorites().map { list ->
            list.map { fav ->
                Track(
                    id = fav.contentUri.hashCode().toLong(),
                    title = fav.title,
                    artist = fav.artist,
                    album = fav.album,
                    durationMs = fav.durationMs,
                    contentUri = fav.contentUri,
                    albumArtUri = fav.albumArtUri,
                    isFavorite = true
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun toggleFavorite(track: Track) = withContext(Dispatchers.IO) {
        if (track.isFavorite) {
            favoriteDao.removeFavorite(track.contentUri)
        } else {
            favoriteDao.insertFavorite(
                FavoriteTrackEntity(
                    contentUri = track.contentUri,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    durationMs = track.durationMs,
                    albumArtUri = track.albumArtUri
                )
            )
        }
    }
}
