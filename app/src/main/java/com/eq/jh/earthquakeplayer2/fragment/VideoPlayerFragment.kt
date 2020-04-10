package com.eq.jh.earthquakeplayer2.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.custom.DraggableLayout
import com.eq.jh.earthquakeplayer2.custom.VideoPlayerControlView
import com.eq.jh.earthquakeplayer2.playback.data.AbstractMusicSource
import com.eq.jh.earthquakeplayer2.playback.player.EarthquakePlayer
import com.google.android.exoplayer2.Player

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

        // related to adapter view type
        private const val VIEW_TYPE_ITEM = 1
    }

    private lateinit var draggableLayout: DraggableLayout
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var controlView: VideoPlayerControlView
    private lateinit var closeIv: ImageView

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

    private fun createPlayer(context: Context) {
        Log.d(TAG, "createPlayer")

        player = EarthquakePlayer(context)
        player?.setCallback(object : EarthquakePlayer.ExoPlayerCallback {
            override fun onCompletion() {
                Log.d(TAG, "onCompletion()")
                // seekTo followed by pause, order is important?
                player?.seekTo(0)
                player?.pause()
                controlView.togglePlayOrPause(false)
            }

            override fun onPlaybackStatusChanged(state: Int) {
                Log.d(TAG, "onPlaybackStatusChanged state : $state")
                if (state == Player.STATE_READY) {
                    if (!isPrepared) {
                        player?.start()
                        controlView.togglePlayOrPause(true)
                        isPrepared = true
                    }
                }
            }

            override fun onError(error: String) {
                Log.d(TAG, "onError error : $error")
            }
        })
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        releasePlayer()
        super.onDestroy()
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

        draggableLayout = view.findViewById(R.id.draggable_layout)
        surfaceView = view.findViewById(R.id.surface_view)
        controlView = view.findViewById(R.id.control_view)
        recyclerView = view.findViewById(R.id.recycler_view)
        closeIv = view.findViewById(R.id.close_iv)

        draggableLayout.addDisableDraggingView(controlView.getSeekBar())
        draggableLayout.setOnDraggableListener(object : DraggableLayout.OnDraggableListener {
            override fun onMaximized() {
                if (closeIv.visibility == View.VISIBLE) {
                    closeIv.visibility = View.INVISIBLE
                }
//                controlView.alpha = 1f
            }

            override fun onMinimized() {
                if (controlView.visibility == View.VISIBLE) {
                    controlView.visibility = View.INVISIBLE
                }
                if (closeIv.visibility == View.INVISIBLE) {
                    closeIv.visibility = View.VISIBLE
                }
//                controlView.alpha = 1f
//                closeIv.alpha = 1f
            }

            override fun onDragStart() {
//                Log.d(TAG, "controlView.visibility : ${controlView.visibility}")
//                Log.d(TAG, "closeIv.visibility : ${closeIv.visibility}")
//
//                controlView.alpha = 0f
//                closeIv.alpha = 0f
                if (closeIv.visibility == View.VISIBLE) {
                    closeIv.visibility = View.INVISIBLE
                }
            }
        })

        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
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

        recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = infoAdapter
            setHasFixedSize(true)
        }

        // scoped storage를 아래처럼 처리해야 하나?
        // Log.d(TAG, "onViewCreated Environment.isExternalStorageLegacy() : ${Environment.isExternalStorageLegacy()}")
        val contentUri = Uri.parse(mediaMetadataCompat?.getString(AbstractMusicSource.CUSTOM_METADATA_TRACK_SOURCE))
        Log.d(TAG, "onViewCreated contentUri : $contentUri")
        player?.setDataSource(contentUri)

        surfaceView.setOnClickListener {
            Log.d(TAG, "surfaceView click")
            if (draggableLayout.isMaximized()) {
                toggleControlView()
            } else if (draggableLayout.isMinimized()) {
                draggableLayout.maximize()
            }
        }

        controlView.setControlViewCallback(object : VideoPlayerControlView.ControlViewCallback {
            override fun onPlayClick() {
                Log.d(TAG, "onPlayClick click")
                val isPlaying = isPlaying()
                performPlayClick(isPlaying)
                controlView.togglePlayOrPause(!isPlaying)
            }
        })

        closeIv.setOnClickListener {
            releasePlayer()
            removeFragment(this, TAG)
            Log.d(TAG, "closeIv click")
        }
    }

    private fun performPlayClick(isPlaying: Boolean) {
        Log.d(TAG, "performPlayClick isPrepared : $isPrepared")
        if (isPrepared) {
            Log.d(TAG, "performPlayClick : $isPlaying")

            if (isPlaying) {
                player?.pause()
            } else {
                player?.start()
            }
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

    private fun toggleControlView() {
        Log.d(TAG, "controlView.visibility : ${controlView.visibility}")
        if (controlView.visibility == View.VISIBLE) {
            controlView.visibility = View.INVISIBLE
        } else if (controlView.visibility == View.INVISIBLE) {
            controlView.visibility = View.VISIBLE
        }
    }

    private inner class InfoAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return 20
        }

        override fun getItemViewType(position: Int): Int {
            return VIEW_TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.list_video_player_info_item, parent, false))
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            when (viewHolder.itemViewType) {
                VIEW_TYPE_ITEM -> {
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