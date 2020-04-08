package com.eq.jh.earthquakeplayer2.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.eq.jh.earthquakeplayer2.BaseActivity

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
open class BaseFragment : Fragment() {
    companion object {
        const val TAG = "BaseFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val instanceState = savedInstanceState ?: arguments
        onRestoreInstanceState(instanceState)
    }

    open fun openFragment(fragment: BaseFragment, tag: String) {
        Log.d(TAG, "open() f:$fragment tag:$tag")

        if (activity is BaseActivity) {
            (activity as BaseActivity)?.let {
                it.addUniqueFragment(fragment, tag)
            }
        }
    }

    fun removeFragment(fragment: BaseFragment, tag: String) {
        if (activity is BaseActivity) {
            (activity as BaseActivity)?.let {
                it.removeFragment(fragment, tag)
            }
        }
    }

    open fun onRestoreInstanceState(inState: Bundle?) {

    }
}