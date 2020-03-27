package com.eq.jh.earthquakeplayer2.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.eq.jh.earthquakeplayer2.R
import kotlinx.android.synthetic.main.titlebar.view.*

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-22
 *
 */
class TitleBar(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.titlebar, this, true)
    }

    fun setTitle(title: String) {
        (title_tv as TextView).text = title
    }
}