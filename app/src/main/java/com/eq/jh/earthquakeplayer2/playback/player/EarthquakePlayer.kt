package com.eq.jh.earthquakeplayer2.playback.player

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import com.eq.jh.earthquakeplayer2.rxbus.RxBus
import com.eq.jh.earthquakeplayer2.rxbus.RxBusEvent
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-31
 *
 */
class EarthquakePlayer(val context: Context) {
    companion object {
        const val TAG = "EarthquakePlayer"
    }

    interface ExoPlayerCallback {
        fun onCompletion()
        fun onExoPlayerPlaybackStatusChanged(state: Int)
        fun onError(error: String)
    }

    private var callback: ExoPlayerCallback? = null

    fun setCallback(callback: ExoPlayerCallback) {
        this.callback = callback
    }

    private val simpleExoPlayer: SimpleExoPlayer by lazy {
        /**
         * default track selector is AdaptiveTrackSelection :
         * A bandwidth based adaptive TrackSelection, whose selected track is updated to be the one of highest quality given the current network conditions and the state of the buffer.
         */
        SimpleExoPlayer.Builder(context).build().apply {
            addListener(eventListener)
        }
    }

    private val eventListener = object : Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException) {
            if (error == null) {
                return
            }

            var errorMsg = ""
            errorMsg = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.message ?: ""
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.message ?: ""
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.message ?: ""
                ExoPlaybackException.TYPE_REMOTE, ExoPlaybackException.TYPE_OUT_OF_MEMORY -> error.message ?: ""
                else -> "Unknown: $error"
            }

            Log.e(TAG, "onPlayerError error msg : $errorMsg")
            callback?.onError(errorMsg)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.d(TAG, "playWhenReady : $playWhenReady, playbackState : $playbackState")
            when (playbackState) {
                Player.STATE_IDLE, Player.STATE_BUFFERING, Player.STATE_READY -> {
                    if (playbackState == Player.STATE_BUFFERING) {
                        RxBus.publish(RxBusEvent.EventLoadingProgress(true))
                    } else if (playbackState == Player.STATE_READY) {
                        RxBus.publish(RxBusEvent.EventLoadingProgress(false))
                    }

                    callback?.onExoPlayerPlaybackStatusChanged(playbackState)
                }
                Player.STATE_ENDED -> {
                    callback?.onCompletion()
                }
            }
        }
    }

    fun getPlayer(): SimpleExoPlayer {
        return simpleExoPlayer
    }

    fun isPlaying(): Boolean {
        return simpleExoPlayer.playWhenReady
    }

    fun setDisplay(sh: SurfaceHolder?) {
        simpleExoPlayer.setVideoSurfaceHolder(sh)
    }

    // Util stuff function
    fun getCurrentPosition(): Long {
        return simpleExoPlayer.currentPosition
    }

    fun getDuration(): Long {
        return simpleExoPlayer.duration
    }

    fun seekTo(positionMs: Long) {
        simpleExoPlayer.seekTo(positionMs)
    }

    fun setVolume(audioVolume: Float) {
        simpleExoPlayer.volume = audioVolume
    }

    fun start() {
        simpleExoPlayer.playWhenReady = true
    }

    fun pause() {
        simpleExoPlayer.playWhenReady = false
    }

    fun stop(reset: Boolean) {
        simpleExoPlayer.stop(reset)
    }

    fun release() {
        simpleExoPlayer.release()
        simpleExoPlayer.removeListener(eventListener)
        // should be null outside of the player
    }

    fun setDataSource(uri: Uri) {
        val mediaSource = buildMediaSource(uri)
        if (mediaSource != null) {
            Log.e(TAG, "MediaSource is ready")
            simpleExoPlayer.prepare(mediaSource)
        } else {
            Log.e(TAG, "MediaSource is not available.")
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource? {
        return when (Util.inferContentType(uri)) {
            C.TYPE_HLS -> {
                HlsMediaSource.Factory(buildDataSourceFactory()).createMediaSource(uri)
            }
            C.TYPE_OTHER -> {
                ProgressiveMediaSource.Factory(buildDataSourceFactory()).createMediaSource(uri)
            }
            else -> {
                Log.d(TAG, "DASH, SS are not supported.")
                null
            }
        }
    }

    private fun buildDataSourceFactory() = DefaultDataSourceFactory(context, Util.getUserAgent(context, "Earthquake2"))
}