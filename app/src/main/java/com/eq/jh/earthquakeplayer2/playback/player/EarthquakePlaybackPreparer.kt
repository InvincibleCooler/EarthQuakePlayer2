package com.eq.jh.earthquakeplayer2.playback.player

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 10/04/2020
 *
 */
class EarthquakePlaybackPreparer(private val player: EarthquakePlayer) : MediaSessionConnector.PlaybackPreparer {
    companion object {
        const val TAG = "PlaybackPreparer"
    }

    override fun getSupportedPrepareActions(): Long {
        return PlaybackStateCompat.ACTION_PREPARE_FROM_URI or
                PlaybackStateCompat.ACTION_PLAY_FROM_URI
    }

    override fun onPrepare(playWhenReady: Boolean) {
        Log.d(TAG, "onPrepare playWhenReady : $playWhenReady")
    }

    /**
     * Handles callbacks to both [MediaSessionCompat.Callback.onPrepareFromMediaId]
     * *AND* [MediaSessionCompat.Callback.onPlayFromMediaId] when using [MediaSessionConnector].
     * This is done with the expectation that "play" is just "prepare" + "play".
     *
     * If your app needs to do something special for either 'prepare' or 'play', it's possible
     * to check [ExoPlayer.getPlayWhenReady]. If this returns `true`, then it's
     * [MediaSessionCompat.Callback.onPlayFromMediaId], otherwise it's
     * [MediaSessionCompat.Callback.onPrepareFromMediaId].
     */
    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle) {
        Log.d(TAG, "onPrepareFromMediaId mediaId : $mediaId, playWhenReady : $playWhenReady")
    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle) {
        Log.d(TAG, "onPrepareFromSearch query : $query, playWhenReady : $playWhenReady")
    }

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle) {
        Log.d(TAG, "onPrepareFromUri uri : $uri, playWhenReady : $playWhenReady")
        player.setDataSource(uri)
    }

    override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle, cb: ResultReceiver): Boolean {
        Log.d(TAG, "onCommand")
        return false
    }
}