package com.eq.jh.earthquakeplayer2.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.eq.jh.earthquakeplayer2.R


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 03/04/2020
 *
 */
class VideoPlayerControlView : LinearLayout, View.OnClickListener {
    interface ControlViewCallback {
        fun onPlayClick() // play pause toggle
    }

    private var listener: ControlViewCallback? = null

    fun setControlViewCallback(listener: ControlViewCallback?) {
        this.listener = listener
    }

    private lateinit var playIv: ImageView

    constructor(context: Context) : super(context) { // 따로 지원 안함
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { // xml 형태로만 지원함
        init()
    }

    private fun init() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.video_player_control_view, this, true)

        playIv = view.findViewById(R.id.play_pause_iv)
        playIv.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.play_pause_iv -> {
                listener?.onPlayClick()
            }
        }
    }

    fun togglePlayOrPause(isPlay: Boolean) {
        playIv.setImageResource(if (isPlay) R.drawable.btn_pause else R.drawable.btn_play)
    }
}