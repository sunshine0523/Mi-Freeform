package com.sunshine.freeform.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.databinding.ActivitySplashBinding
import com.sunshine.freeform.service.ForegroundService
import com.sunshine.freeform.service.KeepAliveService
import com.sunshine.freeform.ui.guide.GuideActivity
import com.sunshine.freeform.ui.main.MainActivity
import com.sunshine.freeform.ui.permission.PermissionActivity
import com.sunshine.freeform.utils.PermissionUtils
import com.sunshine.freeform.utils.ServiceUtils
import com.tencent.bugly.crashreport.CrashReport
import kotlinx.coroutines.*
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuSystemProperties
import rikka.shizuku.SystemServiceHelper

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var viewModel: SplashViewModel
    private lateinit var binding: ActivitySplashBinding
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[SplashViewModel::class.java]

        if (viewModel.getIntSp("version_privacy", -1) < MiFreeform.VERSION_PRIVACY) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.privacy_title))
                .setMessage(getString(R.string.privacy_message))
                .setPositiveButton(getString(R.string.agree)) {_, _ ->
                    viewModel.putIntSp("version_privacy", MiFreeform.VERSION_PRIVACY)

                    CrashReport.initCrashReport(applicationContext)

                    toCheckPermission()
                }
                .setNegativeButton(getString(R.string.reject)) {_, _ ->
                    finish()
                }
                .setCancelable(false)
                .create().show()
        } else {
            toCheckPermission()
        }
    }

    private fun toCheckPermission() {
        if (checkPermission()) {
            //移除了引导界面，但是仍然可以自行查看 q220917.2
            showMain()
//            if (viewModel.getIntSp("version", -1) < MiFreeform.VERSION) {
//                showGuide()
//            } else {
//                showMain()
//            }
        } else {
            showPermission()
        }
    }

    /**
     * 检查米窗所需要的权限
     */
    private fun checkPermission(): Boolean {
        when(viewModel.getIntSp("service_type", KeepAliveService.SERVICE_TYPE)) {
            ForegroundService.SERVICE_TYPE -> {
                if (!ServiceUtils.isServiceWork(this, "com.sunshine.freeform.service.ForegroundService")) {
                    startForegroundService(Intent(this, ForegroundService::class.java))
                }
            }
        }
        return PermissionUtils.checkOverlayPermission(this)
    }

    private fun showGuide() {
        scope.launch(Dispatchers.IO) {
            Thread.sleep(500)
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@SplashActivity, GuideActivity::class.java))
                finish()
            }
        }
    }

    private fun showMain() {
        scope.launch(Dispatchers.IO) {
            Thread.sleep(500)
            withContext(Dispatchers.Main) {
                if (viewModel.getBooleanSp("hide_from_recent", false)) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))
                } else {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                finish()
            }
        }
    }

    private fun showPermission() {
        scope.launch(Dispatchers.IO) {
            Thread.sleep(500)
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@SplashActivity, PermissionActivity::class.java))
                finish()
            }
        }
    }
}