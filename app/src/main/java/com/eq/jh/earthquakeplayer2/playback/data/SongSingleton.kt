package com.eq.jh.earthquakeplayer2.playback.data

import android.support.v4.media.MediaBrowserCompat


/**
 * Copyright (C) 2020 Kakao Inc. All rights reserved.
 *
 * Created by Invincible on 17/04/2020
 *
 */
object SongSingleton {
    private var songList: ArrayList<MediaBrowserCompat.MediaItem>? = null
    private var currentIndex = 0

    fun setSongList(songList: ArrayList<MediaBrowserCompat.MediaItem>?) {
        this.songList = songList
    }

    fun getSongList() = this.songList

    fun setCurrentIndex(currentIndex: Int) {
        this.currentIndex = currentIndex
    }

    fun getCurrentIndex() = this.currentIndex

    fun getSize() = songList?.size ?: 0
}