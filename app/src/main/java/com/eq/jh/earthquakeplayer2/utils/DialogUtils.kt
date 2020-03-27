package com.eq.jh.earthquakeplayer2.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
class DialogUtils {

    companion object {

        /**
         * 기본 다이얼로그
         * @param activity
         * @param title
         * @param msg
         * @return
         */
        @JvmStatic
        fun createCommonDialog(activity: Activity, title: String, msg: String): AlertDialog.Builder {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
            builder.setMessage(msg)
            builder.setCancelable(false)
            return builder
        }

        @JvmStatic
        fun createSingleChoiceDialog(activity: Activity, items: Array<String>, listener: DialogInterface.OnClickListener): AlertDialog.Builder {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("시간 건너뛰기")
            builder.setSingleChoiceItems(items, -1, listener)
            builder.setCancelable(false)
            return builder
        }
    }
}