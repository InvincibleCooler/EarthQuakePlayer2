package com.eq.jh.earthquakeplayer2.playback.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.eq.jh.earthquakeplayer2.constants.KeyConstant
import com.eq.jh.earthquakeplayer2.rxbus.RxBus
import com.eq.jh.earthquakeplayer2.rxbus.RxBusEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-25
 *
 */
class VideoSource(private val context: Context) : AbstractMusicSource(context) {
    companion object {
        const val TAG = "VideoSource"
    }

    private var videoList: List<MediaMetadataCompat> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator() = videoList.iterator()

    override suspend fun load() {
        RxBus.publish(RxBusEvent.EventLoadingProgress(true))
        getVideoListFromDb()?.let {
            videoList = it
            state = STATE_INITIALIZED
        } ?: run {
            videoList = emptyList()
            state = STATE_ERROR
        }
        RxBus.publish(RxBusEvent.EventLoadingProgress(false))
    }

    private val VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    private val videoProjection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.TITLE,
//        MediaStore.Video.Media.BUCKET_ID,
//        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
//        MediaStore.Video.Media.RELATIVE_PATH,
        MediaStore.Video.Media.DURATION
    )

    private suspend fun getVideoListFromDb(): List<MediaMetadataCompat>? {
        return withContext(Dispatchers.IO) {
            //            val selection = MediaStore.Files.FileColumns.MIME_TYPE + " = ?"
//            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp4")
//            val selectionArgs = arrayOf(mimeType)

            val orderBy = MediaStore.Audio.Media.DATE_MODIFIED

            context.contentResolver.query(VIDEO_URI, videoProjection, null, null, "$orderBy DESC").use {
                if (it?.moveToFirst() == true) {
                    return@use generateSequence { if (it.moveToNext()) it else null }
                        .map { videoCursor ->
                            val id = videoCursor.getLong(videoCursor.getColumnIndex(MediaStore.Video.Media._ID))
                            val title = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Video.Media.TITLE))
//                            val bucketId = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID))
//                            val bucketDisplayName = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
//                            val relativePath = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH))
                            val duration = videoCursor.getLong(videoCursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                            val contentUri = ContentUris.withAppendedId(VIDEO_URI, id).toString()

                            Log.d(SongSource.TAG, "------------------")
                            Log.d(SongSource.TAG, "id : $id")
                            Log.d(SongSource.TAG, "title : $title")
//                            Log.d(SongSource.TAG, "bucketId : $bucketId")
//                            Log.d(SongSource.TAG, "bucketDisplayName : $bucketDisplayName")
//                            Log.d(SongSource.TAG, "relativePath : $relativePath")
                            Log.d(SongSource.TAG, "duration : $duration")
                            Log.d(SongSource.TAG, "contentUri : $contentUri")

                            MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id.toString())
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                                .putString(KeyConstant.KEY_CUSTOM_METADATA_TRACK_SOURCE, contentUri)
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
