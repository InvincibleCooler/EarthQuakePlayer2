package com.eq.jh.earthquakeplayer2.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.anim.ZoomOutPageTransformer
import com.eq.jh.earthquakeplayer2.custom.TitleBar
import nl.joery.animatedbottombar.AnimatedBottomBar

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
class MainFragment : BaseFragment() {
    companion object {
        const val TAG = "MainFragment"

        fun newInstance() = MainFragment()
    }

    private lateinit var viewpager2: ViewPager2
    private lateinit var bottomBar: AnimatedBottomBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentList = arrayListOf<Fragment>(SongFragment.newInstance(), VideoFragment.newInstance(), SettingFragment.newInstance())

        viewpager2 = view.findViewById(R.id.viewpager2)
        viewpager2.adapter = MainViewPager2Adapter(fragmentList, requireActivity())
        viewpager2.setPageTransformer(ZoomOutPageTransformer())

        bottomBar = view.findViewById(R.id.bottom_bar)
        bottomBar.setupWithViewPager2(viewpager2)

        view.findViewById<TitleBar>(R.id.title_bar).let {
            it.setTitle(getString(R.string.title_main))
        }
    }

    private inner class MainViewPager2Adapter(private val fragments: ArrayList<Fragment>, fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int) = fragments[position]
    }
}