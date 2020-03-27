package com.eq.jh.earthquakeplayer2.utils

import android.content.Context


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-27
 *
 */
class ScreenUtils {
    companion object {
        fun dipToPixel(context: Context?, dip: Float): Int {
            context?.let {
                val density = it.resources.displayMetrics.density
                return (dip * density).toInt()
            }
            return 0
        }

        fun pixelToDip(context: Context?, pixel: Int): Int {
            context?.let {
                val density = context.resources.displayMetrics.density
                return (pixel.toFloat() / density).toInt()
            }
            return 0
        }
    }

}