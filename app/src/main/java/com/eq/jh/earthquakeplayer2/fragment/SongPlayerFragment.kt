package com.eq.jh.earthquakeplayer2.fragment

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.eq.jh.earthquakeplayer2.R

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
class SongPlayerFragment : BaseFragment() {
    companion object {
        const val TAG = "SongPlayerFragment"
        private const val ARG_MEDIA_META_DATA_COMPAT = "argMediaMetaDataCompat"

        fun newInstance(mediaMetadataCompat: MediaMetadataCompat?): SongPlayerFragment {
            return SongPlayerFragment().also { f ->
                f.arguments = Bundle().also { b ->
                    b.putParcelable(ARG_MEDIA_META_DATA_COMPAT, mediaMetadataCompat)
                }
            }
        }

        private const val UPDATE_INITIAL_INTERNAL: Long = 0
        private const val UPDATE_INTERNAL: Long = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRestoreInstanceState(inState: Bundle?) {
    }

    override fun onSaveInstanceState(outState: Bundle) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_song_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}