package com.eq.jh.earthquakeplayer2.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.constants.ContentType
import com.eq.jh.earthquakeplayer2.constants.KeyConstant
import com.eq.jh.earthquakeplayer2.playback.KEY_MEDIA_METADATA
import com.eq.jh.earthquakeplayer2.playback.MusicService
import com.eq.jh.earthquakeplayer2.rxbus.RxBus
import com.eq.jh.earthquakeplayer2.rxbus.RxBusEvent
import com.eq.jh.earthquakeplayer2.utils.ScreenUtils
import com.eq.jh.earthquakeplayer2.utils.TimeUtils
import com.eq.ljh.flags.constants.MediaBrowserIdConstant

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
class VideoFragment : BaseFragment() {
    companion object {
        const val TAG = "VideoFragment"
        private const val VIEW_TYPE_ITEM = 1

        fun newInstance() = VideoFragment()
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private var mediaBrowser: MediaBrowserCompat? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        val bundle = Bundle().also {
            it.putString(ContentType.EXTRA_CONTENT_TYPE_KEY, ContentType.CONTENT_TYPE_VIDEO)
        }

        activity?.let {
            mediaBrowser = MediaBrowserCompat(it, ComponentName(it, MusicService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    Log.d(TAG, "MediaBrowserCompat.ConnectionCallback onConnected")
                    // update UI list by data from the server. not any more action like play

                    val mediaId = mediaBrowser?.root ?: MediaBrowserIdConstant.MEDIA_BROWSER_ID_EMPTY_ROOT
                    Log.d(TAG, "MediaBrowserCompat.ConnectionCallback onConnected mediaId : $mediaId")

                    mediaBrowser?.subscribe(mediaId, subscriptionCallback)
                }

                override fun onConnectionSuspended() {
                    Log.d(TAG, "MediaBrowserCompat.ConnectionCallback onConnectionSuspended")
                }

                override fun onConnectionFailed() {
                    Log.d(TAG, "MediaBrowserCompat.ConnectionCallback onConnectionFailed")
                }
            }, bundle)

            videoAdapter = VideoAdapter(it)
        }

        // Listen for ProgressEvent only
        RxBus.listen(RxBusEvent.EventLoadingProgress::class.java)
            .subscribe {
                progressBar.visibility = if (it.isShow) View.VISIBLE else View.GONE
            }
    }

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            Log.d(TAG, "MediaBrowserCompat.SubscriptionCallback onChildrenLoaded")
            Log.d(TAG, "parentId : $parentId")
            Log.d(TAG, "children : $children")

            if (MediaBrowserIdConstant.MEDIA_BROWSER_ID_VIDEO == parentId) {
                videoAdapter.setItems(children)
                videoAdapter.notifyDataSetChanged()
            }
        }

        override fun onError(parentId: String) {
            Log.d(TAG, "MediaBrowserCompat.SubscriptionCallback onError")
            Log.d(TAG, "parentId : $parentId")
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")

        val isConnected = mediaBrowser?.isConnected ?: false
        if (!isConnected) {
            mediaBrowser?.connect()
        }
    }

    override fun onStop() {
        super.onStop()

        val isConnected = mediaBrowser?.isConnected ?: false
        if (isConnected) {
            mediaBrowser?.disconnect()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.loading_pb)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = videoAdapter
            setHasFixedSize(true)
        }
    }

    private inner class VideoAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var thumbnailWidth = 0
        private var thumbnailHeight = 0

        init {
            thumbnailWidth = ScreenUtils.dipToPixel(activity, 122f)
            thumbnailHeight = ScreenUtils.dipToPixel(activity, 92f)
        }

        private var items = mutableListOf<MediaBrowserCompat.MediaItem>()

        fun setItems(items: MutableList<MediaBrowserCompat.MediaItem>) {
            this.items = items
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemViewType(position: Int): Int {
            return VIEW_TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.list_video_item, parent, false))
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            when (viewHolder.itemViewType) {
                VIEW_TYPE_ITEM -> {
                    val vh = viewHolder as ItemViewHolder
                    val data = items[position].description.extras?.getParcelable<MediaMetadataCompat>(KEY_MEDIA_METADATA)

                    val videoName = data?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    val duration = data?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0
                    val contentUri = Uri.parse(data?.getString(KeyConstant.KEY_CUSTOM_METADATA_TRACK_SOURCE))

                    activity?.let {
                        val bitmap = it.contentResolver.loadThumbnail(contentUri, Size(thumbnailWidth, thumbnailHeight), null)
                        Glide.with(it).load(bitmap).into(vh.thumbIv)
                    }
                    vh.videoNameTv.text = videoName
                    vh.playtimeTv.text = TimeUtils.milliSecondsToTimer(duration)

                    vh.itemView.setOnClickListener {
                        openFragment(VideoPlayerFragment.newInstance(data), VideoPlayerFragment.TAG)
                    }
                }
            }
        }

        private inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val thumbContainer = view.findViewById(R.id.thumb_container) as View
            val thumbIv: ImageView
            val videoNameTv = view.findViewById(R.id.video_name_tv) as TextView
            val playtimeTv = view.findViewById(R.id.playtime_tv) as TextView

            init {
                thumbIv = thumbContainer.findViewById(R.id.iv_thumb)
            }
        }
    }
}