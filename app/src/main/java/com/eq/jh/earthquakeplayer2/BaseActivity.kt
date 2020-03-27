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
            val sb = StringBuilder("[FragmentManager Stack]--------------------\n")
            val fm = supportFragmentManager
            val count = fm.backStackEntryCount
            for (idx in count - 1 downTo 0) {
                val entry = fm.getBackStackEntryAt(idx)
                val f: Fragment? = fm.findFragmentByTag(entry.name)
                if (f != null) {
                    sb.append("[").append(idx).append("]").append(f).append("\n")
                }
            }
            sb.append("-------------------------------------------")
            Log.d(TAG, "dumpFragmentStack : $sb")
        }
    }

    @Synchronized
    open fun addFragment(fragment: Fragment, fragmentTag: String?) {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.add(R.id.fragment, fragment, fragmentTag)
        ft.commitAllowingStateLoss()
        fm.executePendingTransactions()
    }

    @Synchronized
    open fun replaceFragment(fragment: Fragment, fragmentTag: String?) {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.replace(R.id.fragment, fragment, fragmentTag)
        ft.commitAllowingStateLoss()
        fm.executePendingTransactions()
    }
}
