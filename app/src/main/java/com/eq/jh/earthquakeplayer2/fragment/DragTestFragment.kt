package com.eq.jh.earthquakeplayer2.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eq.jh.earthquakeplayer2.R

/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-21
 *
 */
class DragTestFragment : BaseFragment() {
    companion object {
        const val TAG = "DragTestFragment"
        private const val VIEW_TYPE_ITEM = 1

        fun newInstance() = DragTestFragment()
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var testAdapter: TestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            testAdapter = TestAdapter(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_drag_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = testAdapter
            setHasFixedSize(true)
        }
    }

    private inner class TestAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return 20
        }

        override fun getItemViewType(position: Int): Int {
            return VIEW_TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.list_test_item, parent, false))
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
            when (viewHolder.itemViewType) {
                VIEW_TYPE_ITEM -> {
                    val vh = viewHolder as ItemViewHolder

                    vh.titleTv.text = "Title $position"
                }
            }
        }

        private inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleTv = view.findViewById(R.id.title_tv) as TextView
        }
    }
}