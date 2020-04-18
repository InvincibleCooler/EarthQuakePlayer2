package com.eq.jh.earthquakeplayer2.playback.player

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.eq.jh.earthquakeplayer2.constants.KeyConstant
import com.eq.jh.earthquakeplayer2.playback.KEY_MEDIA_METADATA
import com.eq.jh.earthquakeplayer2.playback.MusicService
import com.eq.jh.earthquakeplayer2.playback.data.SongSingleton
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector


/**
 * Copyright (C) 2020 Kakao Inc. All rights reserved.
 *
 * Created by Invincible on 18/04/2020
 *
 */
class EarthquakeQueueNavigator(private val mediaController: MediaControllerCompat) : MediaSessionConnector.QueueNavigator {
    override fun onSkipToQueueItem(player: Player, controlDispatcher: ControlDispatcher, id: Long) {
    }

    override fun onCurrentWindowIndexChanged(player: Player) {
    }

    override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle, cb: ResultReceiver) = false

    override fun getSupportedQueueNavigatorActions(player: Player) =
        (PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT)

    override fun onSkipToNext(player: Player, controlDispatcher: ControlDispatcher) {
        Log.d(MusicService.TAG, "onSkipToNext")
        val size = SongSingleton.getSize()
        var index = SongSingleton.getCurrentIndex()
        if (index == size - 1) {
            index = 0
        } else {
            index++
        }
        SongSingleton.setCurrentIndex(index)
        prepareMediaSource()
    }

    override fun getActiveQueueItemId(player: Player?) = -1L

    override fun onSkipToPrevious(player: Player, controlDispatcher: ControlDispatcher) {
        Log.d(MusicService.TAG, "onSkipToPrevious")
        val size = SongSingleton.getSize()
        var index = SongSingleton.getCurrentIndex()
        if (index == 0) {
            index = size - 1
        } else {
            index--
        }
        SongSingleton.setCurrentIndex(index)
        prepareMediaSource()
    }

    override fun onTimelineChanged(player: Player) {
    }

    private fun prepareMediaSource() {
        mediaController.transportControls.playFromUri(getCurrentUri(), null)
    }

    private fun getCurrentMediaMetadata(): MediaMetadataCompat? {
        val index = SongSingleton.getCurrentIndex()
        return SongSingleton.getSongList()?.get(index)?.description?.extras?.getParcelable(KEY_MEDIA_METADATA)
    }

    private fun getCurrentUri(): Uri? {
        val data = getCurrentMediaMetadata()?.getString(KeyConstant.KEY_CUSTOM_METADATA_TRACK_SOURCE)
        return if (!data.isNullOrBlank()) {
            Uri.parse(data)
        } else {
            null
        }
    }
}