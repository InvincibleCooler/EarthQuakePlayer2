package com.eq.jh.earthquakeplayer2.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.playback.data.SongSingleton
import com.eq.jh.earthquakeplayer2.playback.extensions.isPlaying
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Copyright (C) 2020 Kakao Inc. All rights reserved.
 *
 * Created by Invincible on 17/04/2020
 *
 */
private const val MODE_READ_ONLY = "r"
const val NOW_PLAYING_CHANNEL: String = "com.eq.jh.earthquakeplayer2.playback.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION: Int = 0xb339

class NotificationBuilder(private val context: Context) {
    companion object {
        private const val TAG = "NotificationBuilder"
    }

    private val platformNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val skipToPreviousAction = NotificationCompat.Action(
        R.drawable.btn_indicator_player_back,
        context.getString(R.string.notification_skip_to_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    )
    private val playAction = NotificationCompat.Action(
        R.drawable.btn_indicator_player_play,
        context.getString(R.string.notification_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)
    )
    private val pauseAction = NotificationCompat.Action(
        R.drawable.btn_indicator_player_pause,
        context.getString(R.string.notification_pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)
    )
    private val skipToNextAction = NotificationCompat.Action(
        R.drawable.btn_indicator_player_next,
        context.getString(R.string.notification_skip_to_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
    )

    private val closeAction = NotificationCompat.Action(
        R.drawable.btn_indicator_player_close,
        context.getString(R.string.notification_close),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)
    )

    private val stopPendingIntent =
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)

    suspend fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }

        val controller = MediaControllerCompat(context, sessionToken)
        val index = SongSingleton.getCurrentIndex()
        val mediaMetadata: MediaMetadataCompat? = SongSingleton.getSongList()?.get(index)?.description?.extras?.getParcelable(KEY_MEDIA_METADATA)
        val playbackState = controller.playbackState
        val albumArtUri = mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
        val songName = mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        val artistName = mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

        val builder = NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)
        Log.d(TAG, "playbackState.isPlaying : ${playbackState.isPlaying}, playbackState.state : ${playbackState.state}")

        builder.addAction(skipToPreviousAction)
        if (playbackState.isPlaying) {
            builder.addAction(pauseAction)
        } else {
            builder.addAction(playAction)
        }
        builder.addAction(skipToNextAction)
        builder.addAction(closeAction)

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(1)
            .setShowCancelButton(true)

        val largeIconBitmap = albumArtUri?.let {
            getBitmapFromAlbumArt(it)
        }

        return builder.setContentIntent(controller.sessionActivity)
            .setContentText(artistName)
            .setContentTitle(songName)
            .setDeleteIntent(stopPendingIntent)
            .setLargeIcon(largeIconBitmap)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun shouldCreateNowPlayingChannel() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
        platformNotificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        val notificationChannel = NotificationChannel(
            NOW_PLAYING_CHANNEL,
            context.getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        platformNotificationManager.createNotificationChannel(notificationChannel)
    }

    private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, MODE_READ_ONLY) ?: return@withContext null
            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            BitmapFactory.decodeFileDescriptor(fileDescriptor).apply {
                parcelFileDescriptor.close()
            }
        }
    }

    private suspend fun getBitmapFromAlbumArt(albumArtUri: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Glide.with(context).asBitmap().load(albumArtUri).submit().get()
            } catch (e: Exception) {
                null
            }
        }
    }
}