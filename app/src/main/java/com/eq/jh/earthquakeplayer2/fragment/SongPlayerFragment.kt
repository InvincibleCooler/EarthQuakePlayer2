package com.eq.jh.earthquakeplayer2.fragment

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.constants.DebugConstant
import com.eq.jh.earthquakeplayer2.constants.KeyConstant
import com.eq.jh.earthquakeplayer2.custom.SongPlayerControlView
import com.eq.jh.earthquakeplayer2.playback.KEY_MEDIA_METADATA
import com.eq.jh.earthquakeplayer2.playback.MusicService
import com.eq.jh.earthquakeplayer2.playback.extensions.currentPlayBackPosition
import com.eq.jh.earthquakeplayer2.playback.extensions.isPlaying
import com.eq.jh.earthquakeplayer2.playback.extensions.stateName
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
class SongPlayerFragment : BaseFragment() {
    companion object {
        const val TAG = "SongPlayerFragment"
        private const val ARG_MEDIA_ITEM_LIST = "argMediaItemList"
        private const val ARG_MEDIA_POSITION = "argMediaPosition"

        fun newInstance(mediaItemList: ArrayList<MediaBrowserCompat.MediaItem>, index: Int): SongPlayerFragment {
            return SongPlayerFragment().also { f ->
                f.arguments = Bundle().also { b ->
                    b.putParcelableArrayList(ARG_MEDIA_ITEM_LIST, mediaItemList)
                    b.putInt(ARG_MEDIA_POSITION, index)
                }
            }
        }

        private const val UPDATE_INITIAL_INTERNAL: Long = 0
        private const val UPDATE_INTERNAL: Long = 1000
    }

    private lateinit var songPlayerAdapter: SongPlayerAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var seekBar: SeekBar
    private lateinit var controlView: SongPlayerControlView

    private var mediaItemList: ArrayList<MediaBrowserCompat.MediaItem>? = null
    private var index = 0

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaController: MediaControllerCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        requireActivity().let {
            mediaBrowser = MediaBrowserCompat(it, ComponentName(it, MusicService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    Log.d(TAG, "onConnected")
                    val sessionToken = mediaBrowser.sessionToken
                    // Get a MediaController for the MediaSession.
                    mediaController = MediaControllerCompat(it, sessionToken).apply {
                        registerCallback(mediaControllerCallback)
                    }
                    val state = mediaController?.playbackState
                    val transportControls = mediaController?.transportControls
                    Log.d(TAG, "MediaBrowserCompat mediaController state : ${state?.stateName}")
                    if (state != null) {
                        val contentUri = getCurrentUri()
                        if (contentUri != null) {
                            transportControls?.playFromUri(contentUri, null)
                        }
                    }
                }

                override fun onConnectionSuspended() {
                    Log.d(TAG, "onConnectionSuspended")
                }

                override fun onConnectionFailed() {
                    Log.d(TAG, "onConnectionFailed")
                }
            }, null)
            mediaBrowser.connect()
        }
        songPlayerAdapter = SongPlayerAdapter(requireActivity())
    }

    /**
     * PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_PAUSED
     * 가 MediaControllerCompat.Callback() 에서 여러번 호출됨. 단 서로 섞이는 경우는 없고 순서대로 발생함
     * 예를 들어서 PlaybackStateCompat.STATE_BUFFERING PlaybackStateCompat.STATE_PLAYING PlaybackStateCompat.STATE_BUFFERING 이런 경우는 없고
     * PlaybackStateCompat.STATE_BUFFERING PlaybackStateCompat.STATE_BUFFERING PlaybackStateCompat.STATE_PLAYING 이런경우는 있음
     * 한번씩만 처리하기 위해서 아래 불린값을 사용함
     */
    private var isAlreadyPlayed = false
    private var isAlreadyStopped = false
    private var isAlreadyPaused = false

    private fun initCount() {
        isAlreadyPlayed = false
        isAlreadyStopped = false
        isAlreadyPaused = false
    }

    /**
     * fragment시작과 동시에 플레이 시키기 위해서
     */
    private var isFirstStarted = false

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (DebugConstant.DEBUG) {
                Log.d(TAG, "MediaControllerCallback onPlaybackStateChanged state : $state")
            }
            when (state?.state) {
                PlaybackStateCompat.STATE_NONE -> {
                    Log.d(TAG, "onPlaybackStateChanged STATE_NONE")
                }
                PlaybackStateCompat.STATE_BUFFERING -> {
                    Log.d(TAG, "onPlaybackStateChanged STATE_BUFFERING")
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    if (!isAlreadyPlayed) {
                        Log.d(TAG, "onPlaybackStateChanged STATE_PLAYING")
                        startUpdateSeekBar()
                        controlView.togglePlayOrPause(true)

                        isAlreadyPlayed = true
                        isAlreadyStopped = false
                        isAlreadyPaused = false
                    }
                }
                PlaybackStateCompat.STATE_STOPPED -> {
                    if (!isAlreadyStopped) {
                        Log.d(TAG, "onPlaybackStateChanged STATE_STOPPED")
                        val size = mediaItemList?.size ?: 0
                        if (index == (size - 1)) {
                            index = 0
                        } else {
                            index++
                        }
                        Log.d(TAG, "STATE_STOPPED size : $size, index : $index")

                        prepareUpdateUi()

                        isAlreadyPlayed = false
                        isAlreadyStopped = true
                        isAlreadyPaused = false
                    }
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    if (!isFirstStarted) {
                        Log.d(TAG, "onPlaybackStateChanged STATE_PAUSED isFirstStarted")
                        isFirstStarted = true
                        mediaController?.transportControls?.play()
                    }

                    if (!isAlreadyPaused) {
                        Log.d(TAG, "onPlaybackStateChanged STATE_PAUSED")
                        controlView.togglePlayOrPause(false)

                        isAlreadyPlayed = false
                        isAlreadyStopped = false
                        isAlreadyPaused = true
                    }
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.d(TAG, "onMetadataChanged metadata : $metadata")
            if (metadata != null) {
                songPlayerAdapter.notifyDataSetChanged()
            }
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            Log.d(TAG, "onQueueChanged")
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            Log.d(TAG, "onSessionEvent")
        }

        override fun onSessionDestroyed() {
            Log.d(TAG, "onSessionDestroyed")
        }
    }

//    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
//        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
//            if (DebugConstant.DEBUG) {
//                Log.d(TAG, "MediaControllerCallback onPlaybackStateChanged state : $state")
//            }
//            when (state?.state) {
//                PlaybackStateCompat.STATE_NONE -> {
//                    Log.d(TAG, "onPlaybackStateChanged STATE_NONE")
//                }
//                PlaybackStateCompat.STATE_BUFFERING -> {
//                    Log.d(TAG, "onPlaybackStateChanged STATE_BUFFERING")
//                }
//                PlaybackStateCompat.STATE_PLAYING -> {
//                    if (!isAlreadyPlayed) {
//                        Log.d(TAG, "onPlaybackStateChanged STATE_PLAYING")
//                        startUpdateSeekBar()
//                        controlView.togglePlayOrPause(true)
//
//                        isAlreadyPlayed = true
//                        isAlreadyStopped = false
//                        isAlreadyPaused = false
//                    }
//                }
//                PlaybackStateCompat.STATE_STOPPED -> {
//                    if (!isAlreadyStopped) {
//                        Log.d(TAG, "onPlaybackStateChanged STATE_STOPPED")
//                        val size = mediaItemList?.size ?: 0
//                        if (index == (size - 1)) {
//                            index = 0
//                        } else {
//                            index++
//                        }
//                        Log.d(TAG, "STATE_STOPPED size : $size, index : $index")
//
//                        prepareUpdateUi()
//
//                        isAlreadyPlayed = false
//                        isAlreadyStopped = true
//                        isAlreadyPaused = false
//                    }
//                }
//                PlaybackStateCompat.STATE_PAUSED -> {
//                    if (!isFirstStarted) {
//                        Log.d(TAG, "onPlaybackStateChanged STATE_PAUSED isFirstStarted")
//                        isFirstStarted = true
//                        mediaController?.transportControls?.play()
//                    }
//
//                    if (!isAlreadyPaused) {
//                        Log.d(TAG, "onPlaybackStateChanged STATE_PAUSED")
//                        controlView.togglePlayOrPause(false)
//
//                        isAlreadyPlayed = false
//                        isAlreadyStopped = false
//                        isAlreadyPaused = true
//                    }
//                }
//            }
//        }
//
//        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
//            Log.d(TAG, "onMetadataChanged metadata : $metadata")
//            if (metadata != null) {
//                songPlayerAdapter.notifyDataSetChanged()
//            }
//        }
//
//        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
//            Log.d(TAG, "onQueueChanged")
//        }
//
//        override fun onSessionEvent(event: String?, extras: Bundle?) {
//            Log.d(TAG, "onSessionEvent")
//        }
//
//        override fun onSessionDestroyed() {
//            Log.d(TAG, "onSessionDestroyed")
//        }
//    }

    override fun onRestoreInstanceState(inState: Bundle?) {
        mediaItemList = inState?.getParcelableArrayList(ARG_MEDIA_ITEM_LIST)
        index = inState?.getInt(ARG_MEDIA_POSITION) ?: 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ARG_MEDIA_ITEM_LIST, mediaItemList)
        outState.putInt(ARG_MEDIA_POSITION, index)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView")
        return inflater.inflate(R.layout.fragment_song_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = songPlayerAdapter
            setHasFixedSize(true)
        }
    }

    private inner class SongPlayerAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val viewTypeItem = 1

        override fun getItemCount() = 1

        override fun getItemViewType(position: Int): Int {
            return viewTypeItem
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.song_player_item, parent, false))
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            when (viewHolder.itemViewType) {
                viewTypeItem -> {
                    val vh = viewHolder as ItemViewHolder
                    val data = getCurrentMediaMetadata()

                    val albumArtUri = data?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
                    val songName = data?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    val artistName = data?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

//                    context?.let {
//                        Glide.with(it).load(albumArtUri).into(vh.thumbIv)
//                    }

                    vh.songNameTv.text = songName
                    vh.artistNameTv.text = artistName
                    seekBar.max = getCurrentDuration().toInt()
                    vh.totalTimeTv.text = DateUtils.formatElapsedTime(getCurrentDuration() / 1000)
                }
            }
        }

        private inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val thumbContainer = view.findViewById(R.id.thumb_container) as View
            val thumbIv: ImageView
            val songNameTv = view.findViewById(R.id.song_name_tv) as TextView
            val artistNameTv = view.findViewById(R.id.artist_name_tv) as TextView
            val updateTimeTv = view.findViewById(R.id.update_time_tv) as TextView
            val totalTimeTv = view.findViewById(R.id.total_time_tv) as TextView

            init {
                thumbIv = thumbContainer.findViewById(R.id.iv_thumb)
                seekBar = view.findViewById(R.id.seek_bar)
                controlView = view.findViewById(R.id.control_view)

                controlView.run {
                    setSongControlViewCallback(object : SongPlayerControlView.SongControlViewCallback {
                        override fun onPreviousClick() {
                            val size = mediaItemList?.size ?: 0
                            if (index == 0) {
                                index = size - 1
                            } else {
                                index--
                            }
                            prepareUpdateUi()
                        }

                        override fun onPlayClick() {
                            val playbackState = mediaController?.playbackState
                            Log.d(TAG, "onPlayClick playbackState.isPlaying : ${playbackState?.isPlaying}")

                            if (playbackState?.isPlaying == true) {
                                mediaController?.transportControls?.pause()
                            } else {
                                mediaController?.transportControls?.play()
                            }
                        }

                        override fun onNextClick() {
                            val size = mediaItemList?.size ?: 0
                            if (index == size - 1) {
                                index = 0
                            } else {
                                index++
                            }
                            prepareUpdateUi()
                        }

                    })
                }

                seekBar.run {
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                            if (DebugConstant.DEBUG) {
                                Log.d(TAG, "onProgressChanged progress : $progress")
                            }
                            updateTimeTv.text = DateUtils.formatElapsedTime(progress.toLong() / 1000)
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                            stopUpdateSeekBar()
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            initCount()
                            mediaController?.transportControls?.seekTo(seekBar.progress.toLong())
                        }
                    })
                }
            }
        }
    }

    private fun prepareUpdateUi() {
        mediaController?.transportControls?.playFromUri(getCurrentUri(), null)
    }

    private fun getCurrentMediaMetadata(): MediaMetadataCompat? {
        return mediaItemList?.get(index)?.description?.extras?.getParcelable(KEY_MEDIA_METADATA)
    }

    private fun getCurrentUri(): Uri? {
        val data = getCurrentMediaMetadata()?.getString(KeyConstant.KEY_CUSTOM_METADATA_TRACK_SOURCE)
        return if (!data.isNullOrBlank()) {
            Uri.parse(data)
        } else {
            null
        }
    }

    private fun getCurrentMediaId(): String? {
        return getCurrentMediaMetadata()?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    }

    private fun getCurrentDuration(): Long {
        return getCurrentMediaMetadata()?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0
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
        val currentPosition = mediaController?.playbackState?.currentPlayBackPosition?.toInt() ?: 0
        if (DebugConstant.DEBUG) {
            Log.d(TAG, "currentPosition : $currentPosition")
        }

        seekBar.progress = currentPosition
    }

    private fun stopUpdateSeekBar() {
        scheduleFuture?.cancel(false)
    }

    override fun onStart() {
        super.onStart()
        if (mediaController?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            startUpdateSeekBar()
        }
    }

    override fun onStop() {
        super.onStop()
        stopUpdateSeekBar()
    }

    override fun onDestroy() {
        if (!scheduleExecutor.isShutdown) {
            scheduleExecutor.shutdown()
        }
        mediaController?.unregisterCallback(mediaControllerCallback)

        super.onDestroy()
    }
}