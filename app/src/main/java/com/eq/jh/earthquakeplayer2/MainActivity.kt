package com.eq.jh.earthquakeplayer2

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.eq.jh.earthquakeplayer2.fragment.MainFragment
import com.eq.jh.earthquakeplayer2.permissions.PermissionConstants
import com.eq.jh.earthquakeplayer2.utils.DialogUtils
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.toObservable
import nl.joery.animatedbottombar.AnimatedBottomBar

class MainActivity : BaseActivity() {
    lateinit var bottomBar: AnimatedBottomBar

    private val permissionList = arrayOf(
        PermissionConstants.PERMISSIONS_READ_EXTERNAL_STORAGE
    )

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomBar = findViewById(R.id.bottom_bar)

        requestPermission(permissionList)
    }

    @SuppressLint("CheckResult")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Observables.zip(permissions.toObservable(), grantResults.toObservable())
            .filter {
                it.second != PackageManager.PERMISSION_GRANTED
            }
            .count()
            .subscribe { permissionCount: Long?, _: Throwable? ->
                if (permissionCount == 0L) { //zero means all permissions granted
                    initView()
                } else {
                    val title = getString(R.string.dialog_notice)
                    val body = getString(R.string.dialog_permission_body)
                    val builder: AlertDialog.Builder = DialogUtils.createCommonDialog(this@MainActivity, title, body)
                    builder.setPositiveButton(getString(R.string.dialog_request)) { _, _ -> requestPermission(permissionList) }
                    builder.setNegativeButton(getString(R.string.dialog_cancel)) { _, _ -> finish() }
                    builder.show()
                }
            }
    }

    private fun initView() {
        addUniqueFragment(MainFragment.newInstance(), MainFragment.TAG)
    }

    @SuppressLint("CheckResult")
    private fun requestPermission(permissionArray: Array<String>) {
        permissionArray.toObservable()
            .filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            .toList()
            .map {
                permissionArray.sortedArray()
            }
            .subscribe { permissions: Array<String>?, _: Throwable? ->
                permissions?.let {
                    ActivityCompat.requestPermissions(this, it, 0)
                }
            }
    }
}
