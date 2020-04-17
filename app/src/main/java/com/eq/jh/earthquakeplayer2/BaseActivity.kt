package com.eq.jh.earthquakeplayer2

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.eq.jh.earthquakeplayer2.constants.DebugConstant

open class BaseActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener {
    companion object {
        private const val TAG = "BaseActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        supportFragmentManager.removeOnBackStackChangedListener(this)
    }

    override fun onBackStackChanged() {
        if (DebugConstant.DEBUG) {
            val sb = StringBuilder("\n[FragmentManager Stack]--------------------\n")
            val fm = supportFragmentManager
            val count = fm.backStackEntryCount
            for (idx in count - 1 downTo 0) {
                val entry = fm.getBackStackEntryAt(idx)
                val f: Fragment? = fm.findFragmentByTag(entry.name)
                if (f != null) {
                    sb.append("[").append(idx).append("]").append(entry.name).append("\n")
                }
            }
            sb.append("-------------------------------------------")
            Log.d(TAG, "dumpFragmentStack : $sb")
        }
    }

    open fun addFragment(fragment: Fragment, tag: String) {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.add(R.id.fragment, fragment, tag)
        ft.addToBackStack(tag)
        ft.commitAllowingStateLoss()
        fm.executePendingTransactions()
    }

    open fun replaceFragment(fragment: Fragment, tag: String) {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.fragment, fragment, tag)
        ft.addToBackStack(tag)
        ft.commitAllowingStateLoss()
        fm.executePendingTransactions()
    }

    /**
     * 하나의 프래그먼트는 하나만 추가되도록 한다. 중복방지
     */
    open fun addUniqueFragment(fragment: Fragment, tag: String) {
        removeFragment(fragment, tag)
        addFragment(fragment, tag)
    }

    open fun removeFragment(fragment: Fragment, tag: String) {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()

        val count = fm.backStackEntryCount
        if (DebugConstant.DEBUG) {
            Log.d(TAG, "count : $count")
        }

        if (count > 0) {
            for (i in (count - 1) downTo 0) {
                val entry = fm.getBackStackEntryAt(i)
                if (tag == entry.name) {
                    ft.remove(fragment)
                    ft.commitAllowingStateLoss()
                    fm.popBackStackImmediate(entry.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    break
                }
            }
        }
    }
}
