package com.eq.jh.earthquakeplayer2.playback.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.webkit.MimeTypeMap
import com.eq.jh.earthquakeplayer2.rxbus.RxBus
import com.eq.jh.earthquakeplayer2.rxbus.RxBusEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-23
 *
 */
class SongSource(private val context: Context) : AbstractMusicSource(context) {
    companion object {
        const val TAG = "SongSource"
    }

    private var songList: List<MediaMetadataCompat> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator() = songList.iterator()

    override suspend fun load() {
        RxBus.publish(RxBusEvent.EventLoadingProgress(true))
        getSongListFromDb()?.let {
            songList = it
            state = STATE_INITIALIZED
        } ?: run {
            songList = emptyList()
            state = STATE_ERROR
        }
        RxBus.publish(RxBusEvent.EventLoadingProgress(false))
    }

    private val AUDIO_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI // content://media/external/audio/media
    private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
    private val GENRE_URI = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI

    private val mSongProjection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Artists.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Albums.ALBUM_ID,  // album_art
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.DURATION
    )

    private suspend fun getSongListFromDb(): List<MediaMetadataCompat>? {
        return withContext(Dispatchers.IO) {
            val selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ?"
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
            val selectionArgs = arrayOf(mimeType)
            //        String orderBy = MediaStore.Audio.Media.DATE_MODIFIED;
            val orderBy = MediaStore.Audio.Media.TITLE

            Log.d(TAG, "AUDIO_URI : ${AUDIO_URI.toString()}")
            Log.d(TAG, "GENRE_URI : ${GENRE_URI.toString()}")

            context.contentResolver.query(AUDIO_URI, mSongProjection, selection, selectionArgs, "$orderBy ASC").use {
                if (it?.moveToFirst() == true) {
                    return@use generateSequence { if (it.moveToNext()) it else null }
                        .map { audioCursor ->
                            val id = audioCursor.getLong(audioCursor.getColumnIndex(MediaStore.Audio.Media._ID))
                            val title = audioCursor.getString(audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                            val artist = audioCursor.getString(audioCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST))
                            val album = audioCursor.getString(audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                            val albumId = audioCursor.getLong(audioCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID))
                            val track = audioCursor.getLong(audioCursor.getColumnIndex(MediaStore.Audio.Media.TRACK))
                            val duration = audioCursor.getLong(audioCursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                            val contentUri = ContentUris.withAppendedId(AUDIO_URI, id).toString()
                            val albumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()

                            val uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", id.toInt())
                            var genreName = ""

                            context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Genres.NAME), null, null, null).use { genreCursor ->
                                if (genreCursor?.moveToFirst() == true) {
                                    genreName = genreCursor.getString(genreCursor.getColumnIndex(MediaStore.Audio.Genres.NAME))
                                }
                            }

                            Log.d(TAG, "------------------")
                            Log.d(TAG, "id : $id")
                            Log.d(TAG, "title : $title")
                            Log.d(TAG, "artist : $artist")
                            Log.d(TAG, "album : $album")
                            Log.d(TAG, "albumId : $albumId")
                            Log.d(TAG, "track : $track")
                            Log.d(TAG, "duration : $duration")
                            Log.d(TAG, "contentUri : $contentUri")
                            Log.d(TAG, "albumArtUri : $albumArtUri")
                            Log.d(TAG, "genreName : $genreName")

                            MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id.toString())
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                                .putString(CUSTOM_METADATA_TRACK_SOURCE, contentUri)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArtUri)
                                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genreName)
                                .build()
                        }
                        .toList()
                } else {
                    null
                }
            }
        }
    }
}
