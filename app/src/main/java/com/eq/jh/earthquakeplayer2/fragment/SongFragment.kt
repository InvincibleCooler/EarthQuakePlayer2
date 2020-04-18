package com.eq.jh.earthquakeplayer2.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
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
import com.eq.jh.earthquakeplayer2.playback.data.SongSingleton
import com.eq.jh.earthquakeplayer2.rxbus.RxBus
import com.eq.jh.earthquakeplayer2.rxbus.RxBusEvent
import com.eq.ljh.flags.constants.MediaBrowserIdConstant

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
class SongFragment : BaseFragment() {
    companion object {
        const val TAG = "SongFragment"
        fun newInstance() = SongFragment()
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private lateinit var mediaBrowser: MediaBrowserCompat

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        val bundle = Bundle().also {
            it.putString(ContentType.EXTRA_CONTENT_TYPE_KEY, ContentType.CONTENT_TYPE_SONG)
        }

        requireActivity().let {
            mediaBrowser = MediaBrowserCompat(it, ComponentName(it, MusicService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    Log.d(TAG, "onConnected")
                    // update UI list by data from the server. not any more action like play

                    val mediaId = mediaBrowser.root
                    Log.d(TAG, "onConnected mediaId : $mediaId")

                    mediaBrowser.subscribe(mediaId, subscriptionCallback)
                }

                override fun onConnectionSuspended() {
                    Log.d(TAG, "onConnectionSuspended")
                }

                override fun onConnectionFailed() {
                    Log.d(TAG, "onConnectionFailed")
                }
            }, bundle)
            mediaBrowser.connect()
            songAdapter = SongAdapter(it)
        }

        // Listen for ProgressEvent only
        RxBus.listen(RxBusEvent.EventLoadingProgress::class.java)
            .subscribe {
                progressBar.visibility = if (it.isShow) View.VISIBLE else View.GONE
            }
    }

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            Log.d(TAG, "onChildrenLoaded")
            Log.d(TAG, "parentId : $parentId")
            Log.d(TAG, "children : $children")

            if (MediaBrowserIdConstant.MEDIA_BROWSER_ID_SONG == parentId) {
                SongSingleton.setSongList(children as ArrayList)
                songAdapter.setItems(children)
                songAdapter.notifyDataSetChanged()
            }
        }

        override fun onError(parentId: String) {
            Log.d(TAG, "onError")
            Log.d(TAG, "parentId : $parentId")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if (mediaBrowser.isConnected) {
            mediaBrowser.disconnect()
        }
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_song, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.loading_pb)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
            setHasFixedSize(true)
        }
    }

    private inner class SongAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val viewTypeItem = 1

        private var items = mutableListOf<MediaBrowserCompat.MediaItem>()

        fun setItems(items: MutableList<MediaBrowserCompat.MediaItem>) {
            this.items = items
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemViewType(position: Int): Int {
            return viewTypeItem
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.list_song_item, parent, false))
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            when (viewHolder.itemViewType) {
                viewTypeItem -> {
                    val vh = viewHolder as ItemViewHolder
                    val data = items[position].description.extras?.getParcelable<MediaMetadataCompat>(KEY_MEDIA_METADATA)

                    val songName = data?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    val artistName = data?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                    val contentUri = Uri.parse(data?.getString(KeyConstant.KEY_CUSTOM_METADATA_TRACK_SOURCE))
                    val albumArtUri = data?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)

                    Glide.with(requireActivity()).load(albumArtUri).into(vh.thumbIv)

                    vh.songNameTv.text = songName
                    vh.artistNameTv.text = artistName

                    vh.itemView.setOnClickListener {
                        SongSingleton.setCurrentIndex(position)
                        openFragment(SongPlayerFragment.newInstance(), SongPlayerFragment.TAG)
                    }
                }
            }
        }

        private inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val thumbContainer = view.findViewById(R.id.thumb_container) as View
            val thumbIv: ImageView
            val songNameTv = view.findViewById(R.id.song_name_tv) as TextView
            val artistNameTv = view.findViewById(R.id.artist_name_tv) as TextView

            init {
                thumbIv = thumbContainer.findViewById(R.id.iv_thumb)
            }
        }
    }
}