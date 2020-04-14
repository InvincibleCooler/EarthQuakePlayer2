package com.eq.jh.earthquakeplayer2.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import com.eq.jh.earthquakeplayer2.R
import kotlinx.android.synthetic.main.song_player_control_view.view.*


/**
 * Copyright (C) 2020 Kakao Inc. All rights reserved.
 *
 * Created by Invincible on 14/04/2020
 *
 */
class SongPlayerControlView : LinearLayout {
    companion object {
        const val TAG = "SongPlayerControlView"
    }

    interface SongControlViewCallback {
        fun onPreviousClick()
        fun onRewindClick()
        fun onPlayClick()
        fun onFastForwardClick()
        fun onNextClick()
    }

    private var listener: SongControlViewCallback? = null

    fun setSongControlViewCallback(listener: SongControlViewCallback?) {
        this.listener = listener
    }

    private lateinit var playIv: ImageView

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { // xml 형태로만 지원함
        init()
    }

    private fun init() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.song_player_control_view, this, true)
        playIv = view.findViewById(R.id.play_iv)

        view.previous_layout.setOnClickListener {
            listener?.onPreviousClick()
        }

        view.rewind_layout.setOnClickListener {
            listener?.onRewindClick()
        }

        view.play_layout.setOnClickListener {
            listener?.onPlayClick()
        }

        view.ff_layout.setOnClickListener {
            listener?.onFastForwardClick()
        }

        view.next_layout.setOnClickListener {
            listener?.onNextClick()
        }
    }

    fun togglePlayOrPause(isPlaying: Boolean) {
        playIv.setImageResource(if (isPlaying) R.drawable.pause else R.drawable.play)
    }
}