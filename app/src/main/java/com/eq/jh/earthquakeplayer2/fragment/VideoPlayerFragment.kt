package com.eq.jh.earthquakeplayer2.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.constants.KeyConstant
import com.eq.jh.earthquakeplayer2.custom.VideoPlayerControlView
import com.eq.jh.earthquakeplayer2.custom.YoutubeLayout
import com.eq.jh.earthquakeplayer2.playback.player.EarthquakePlayer
import com.google.android.exoplayer2.Player
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
class VideoPlayerFragment : BaseFragment() {
    companion object {
        const val TAG = "VideoPlayerFragment"
        private const val ARG_MEDIA_META_DATA_COMPAT = "argMediaMetaDataCompat"

        fun newInstance(mediaMetadataCompat: MediaMetadataCompat?): VideoPlayerFragment {
            return VideoPlayerFragment().also { f ->
                f.arguments = Bundle().also { b ->
                    b.putParcelable(ARG_MEDIA_META_DATA_COMPAT, mediaMetadataCompat)
                }
            }
        }

        private const val UPDATE_INITIAL_INTERNAL: Long = 0
        private const val UPDATE_INTERNAL: Long = 1000
    }

    // 상단뷰
    private lateinit var youtubeLayout: YoutubeLayout
    private lateinit var surfaceView: SurfaceView
    private lateinit var controlView: VideoPlayerControlView
    private lateinit var closeIv: ImageView

    // 하단뷰
    private lateinit var recyclerView: RecyclerView
    private lateinit var infoAdapter: InfoAdapter

    private var player: EarthquakePlayer? = null
    private var mediaMetadataCompat: MediaMetadataCompat? = null
    private var isPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            createPlayer(it)
            infoAdapter = InfoAdapter(it)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        releasePlayer()
        super.onDestroy()
    }

    private fun createPlayer(context: Context) {
        Log.d(TAG, "createPlayer")

        player = EarthquakePlayer(context).also {
            it.setCallback(object : EarthquakePlayer.ExoPlayerCallback {
                override fun onCompletion() {
                    Log.d(TAG, "onCompletion()")
                    // seekTo followed by pause, order is important?
                    player?.seekTo(0)
                    player?.pause()
                    controlView.togglePlayOrPause(false)
                }

                override fun onExoPlayerPlaybackStatusChanged(state: Int) {
                    Log.d(TAG, "onExoPlayerPlaybackStatusChanged state : $state")
                    if (state == Player.STATE_READY) {
                        if (!isPrepared) {
                            player?.start()
                            isPrepared = true

                            // UI
                            controlView.togglePlayOrPause(true)
                            setDuration()
                        }
                        startUpdateSeekBar()
                    }
                }

                override fun onError(error: String) {
                    Log.d(TAG, "onError error : $error")
                }
            })
        }
    }

    private fun setDuration() {
        val duration = mediaMetadataCompat?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0
        controlView.getSeekBar().max = duration.toInt()
        controlView.setTotalTime(duration)
    }

    override fun onRestoreInstanceState(inState: Bundle?) {
        mediaMetadataCompat = inState?.getParcelable(ARG_MEDIA_META_DATA_COMPAT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(ARG_MEDIA_META_DATA_COMPAT, mediaMetadataCompat)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_video_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        youtubeLayout = view.findViewById(R.id.youtube_layout)
        surfaceView = view.findViewById(R.id.surface_view)
        controlView = view.findViewById(R.id.control_view)
        recyclerView = view.findViewById(R.id.recycler_view)
        closeIv = view.findViewById(R.id.close_iv)

        youtubeLayout.addDisableDraggingView(controlView.getSeekBar())
        youtubeLayout.setOnDraggableListener(object : YoutubeLayout.OnDraggableListener {
            override fun onMaximized() {
                if (closeIv.visibility == View.VISIBLE) {
                    closeIv.visibility = View.INVISIBLE
                }
            }

            override fun onMinimized() {
                if (controlView.visibility == View.VISIBLE) {
                    controlView.visibility = View.INVISIBLE
                }
                if (closeIv.visibility == View.INVISIBLE) {
                    closeIv.visibility = View.VISIBLE
                }
            }

            override fun onDragStart() {
                if (closeIv.visibility == View.VISIBLE) {
                    closeIv.visibility = View.INVISIBLE
                }
            }
        })

        (surfaceView.holder).run {
            addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder?) {
                    player?.setDisplay(holder)
                }

                override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                    player?.setDisplay(holder)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder?) {
                    player?.setDisplay(null)
                }
            })
        }

        recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = infoAdapter
            setHasFixedSize(true)
        }

        // scoped storage를 아래처럼 처리해야 하나?
        // Log.d(TAG, "onViewCreated Environment.isExternalStorageLegacy() : ${Environment.isExternalStorageLegacy()}")
        val contentUri = Uri.parse(mediaMetadataCompat?.getString(KeyConstant.KEY_CUSTOM_METADATA_TRACK_SOURCE))
        Log.d(TAG, "onViewCreated contentUri : $contentUri")
        player?.setDataSource(contentUri)

        surfaceView.setOnClickListener {
            Log.d(TAG, "surfaceView click")
            if (youtubeLayout.isMaximized()) {
                toggleControlView()
            } else if (youtubeLayout.isMinimized()) {
                youtubeLayout.maximize()
            }
        }

        controlView.setControlViewCallback(object : VideoPlayerControlView.ControlViewCallback {
            override fun onPlayClick() {
                val isPlaying = isPlaying()
                if (isPrepared) {
                    if (isPlaying) {
                        player?.pause()
                    } else {
                        player?.start()
                    }
                }
                controlView.togglePlayOrPause(!isPlaying)
            }
        })

        controlView.getSeekBar().run {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    Log.d(TAG, "onProgressChanged progress : $progress")
                    controlView.setUpdateTime(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    stopUpdateSeekBar()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    player?.seekTo(seekBar.progress.toLong())
                }
            })
        }

        closeIv.setOnClickListener {
            releasePlayer()
            removeFragment(this, TAG)
            Log.d(TAG, "closeIv click")
        }
    }

    private fun toggleControlView() {
        Log.d(TAG, "controlView.visibility : ${controlView.visibility}")
        if (controlView.visibility == View.VISIBLE) {
            controlView.visibility = View.INVISIBLE
        } else if (controlView.visibility == View.INVISIBLE) {
            controlView.visibility = View.VISIBLE
        }
    }

    private fun isPlaying(): Boolean {
        return player?.isPlaying() ?: false
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        isPrepared = false
    }

    private val handler = Handler()
    private val scheduleExecutor = Executors.newSingleThreadScheduledExecutor()
    private var scheduleFuture: ScheduledFuture<*>? = null

    private fun startUpdateSeekBar() {
        stopUpdateSeekBar()
        if (!scheduleExecutor.isShutdown) {
            scheduleFuture = scheduleExecutor.scheduleAtFixedRate({
                handler.post { updateProgress() }
            }, UPDATE_INITIAL_INTERNAL, UPDATE_INTERNAL, TimeUnit.MILLISECONDS)
        }
    }

    private fun updateProgress() {
        val currentPosition = player?.getCurrentPosition()?.toInt() ?: 0
        Log.d(TAG, "currentPosition : $currentPosition")

        controlView.getSeekBar().progress = currentPosition
    }

    private fun stopUpdateSeekBar() {
        scheduleFuture?.cancel(false)
    }


    private inner class InfoAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val viewTypeItem = 1

        override fun getItemCount(): Int {
            return 20
        }

        override fun getItemViewType(position: Int): Int {
            return viewTypeItem
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.list_video_player_info_item, parent, false))
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            when (viewHolder.itemViewType) {
                viewTypeItem -> {
                    val vh = viewHolder as ItemViewHolder

                    vh.titleTv.text = "Title $position"

                    vh.itemView.setOnClickListener {
                        Log.d(TAG, "click : ${vh.titleTv.text}")
                    }
                }
            }
        }

        private inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleTv = view.findViewById(R.id.title_tv) as TextView
        }
    }
}