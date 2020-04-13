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
        private const val VIEW_TYPE_ITEM = 1

        fun newInstance() = SongFragment()
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private var mMediaBrowser: MediaBrowserCompat? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        val bundle = Bundle().also {
            it.putString(ContentType.EXTRA_CONTENT_TYPE_KEY, ContentType.CONTENT_TYPE_SONG)
        }

        activity?.let {
            mMediaBrowser = MediaBrowserCompat(it, ComponentName(it, MusicService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    Log.d(TAG, "MediaBrowserCompat.ConnectionCallback onConnected")
                    // update UI list by data from the server. not any more action like play

                    val mediaId = mMediaBrowser?.root ?: MediaBrowserIdConstant.MEDIA_BROWSER_ID_EMPTY_ROOT
                    Log.d(TAG, "MediaBrowserCompat.ConnectionCallback onConnected mediaId : $mediaId")

                    mMediaBrowser?.subscribe(mediaId, subscriptionCallback)

                    // 아래 주석은 내용은 플레이 부분에서 사용할것 같음

                    // Unsubscribe before subscribing is required if this mediaId already has a subscriber
                    // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
                    // the callback, but won't trigger the initial callback.onChildrenLoaded.
                    //
                    // This is temporary: A bug is being fixed that will make subscribe
                    // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
                    // subscriber or not. Currently this only happens if the mediaID has no previous
                    // subscriber or if the media content changes on the service side, so we need to
                    // unsubscribe first.


//                    // Get a MediaController for the MediaSession.
//                    mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
//                        registerCallback(MediaControllerCallback())
//                    }

//                    // 미디어 세션으로 부터 토큰을 얻는다
//                    mMediaBrowser.sessionToken.also { token ->
//                        // MediaControllerCompat을 생성한다.
//                        val mediaController = MediaControllerCompat(this@MediaPlayerActivity, token)
//                        // 컨트롤러를 저장한다
//                        MediaControllerCompat.setMediaController(this@MediaPlayerActivity, mediaController)
//                    }
//                    // UI 만들기를 끝마친다
                }

                override fun onConnectionSuspended() {
                    Log.d(TAG, "MediaBrowserCompat.ConnectionCallback onConnectionSuspended")
                }

                override fun onConnectionFailed() {
                    Log.d(TAG, "MediaBrowserCompat.ConnectionCallback onConnectionFailed")
                }
            }, bundle)
        }

        activity?.let {
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
            Log.d(TAG, "MediaBrowserCompat.SubscriptionCallback onChildrenLoaded")
            Log.d(TAG, "parentId : $parentId")
            Log.d(TAG, "children : $children")

            if (MediaBrowserIdConstant.MEDIA_BROWSER_ID_SONG == parentId) {
                songAdapter.setItems(children)
                songAdapter.notifyDataSetChanged()
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

        val isConnected = mMediaBrowser?.isConnected ?: false
        if (!isConnected) {
            mMediaBrowser?.connect()
        }
    }

    override fun onStop() {
        super.onStop()

        val isConnected = mMediaBrowser?.isConnected ?: false
        if (isConnected) {
            mMediaBrowser?.disconnect()
        }
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
            return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.list_song_item, parent, false))
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            when (viewHolder.itemViewType) {
                VIEW_TYPE_ITEM -> {
                    val vh = viewHolder as ItemViewHolder
                    val data = items[position].description.extras?.getParcelable<MediaMetadataCompat>(KEY_MEDIA_METADATA)

                    val songName = data?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    val artistName = data?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                    val contentUri = Uri.parse(data?.getString(KeyConstant.KEY_CUSTOM_METADATA_TRACK_SOURCE))
                    val albumArtUri = data?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)

                    activity?.let {
                        Glide.with(it).load(albumArtUri).into(vh.thumbIv)
                    }
                    vh.songNameTv.text = songName
                    vh.artistNameTv.text = artistName

                    vh.itemView.setOnClickListener {

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