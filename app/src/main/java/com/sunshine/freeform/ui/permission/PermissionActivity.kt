package com.sunshine.freeform.ui.permission

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.databinding.ActivityPermissionBinding
import com.sunshine.freeform.hook.utils.HookTest
import com.sunshine.freeform.ui.splash.SplashActivity
import com.sunshine.freeform.utils.PermissionUtils
import rikka.sui.Sui


class PermissionActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityPermissionBinding
    private lateinit var accessibilityRFAR: ActivityResultLauncher<Intent>
    private lateinit var overlayRFAR: ActivityResultLauncher<Intent>
    private lateinit var notificationRFAR: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        title = getString(R.string.label_permission)

        accessibilityRFAR = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkKeepAliveService()
        }
        overlayRFAR = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkOverlayPermission()
        }
        notificationRFAR = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkNotificationPermission()
        }

        binding.content.materialCardViewOverlayInfo.setOnClickListener(this)
        binding.content.materialCardViewXposedInfo.setOnClickListener(this)
        binding.content.materialCardViewAccessibilityInfo.setOnClickListener(this)
        binding.content.materialCardViewShizukuInfo.setOnClickListener(this)
        binding.content.materialCardViewNotificationInfo.setOnClickListener(this)

        checkPermission()

        binding.fab.setOnClickListener {
            if (checkPermission()) {
                startActivity(Intent(this, SplashActivity::class.java))
                finish()
            } else {
                Snackbar
                    .make(binding.root, getString(R.string.no_permission), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.fab)
                    .show()
            }
        }
    }

    private fun checkPermission(): Boolean {
        checkXposedPermission()
        checkNotificationPermission()
        checkKeepAliveService()

        val r1 = checkShizukuPermission()
        val r3 = checkOverlayPermission()

        return r1 && r3
    }

    private fun checkOverlayPermission(): Boolean {
        val result = PermissionUtils.checkOverlayPermission(this)
        if (result) {
            binding.content.infoOverlayBg.setBackgroundColor(getColor(R.color.success_color))
            binding.content.imageViewOverlayService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_done))
            binding.content.textViewOverlayServiceInfo.text = getString(R.string.overlay_start)
        } else {
            binding.content.infoOverlayBg.setBackgroundColor(getColor(R.color.warn_color))
            binding.content.imageViewOverlayService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_error_white))
            binding.content.textViewOverlayServiceInfo.text = getString(R.string.overlay_no_start)
        }
        return result
    }

    private fun checkKeepAliveService(): Boolean {
        val result = PermissionUtils.isAccessibilitySettingsOn(this)
        if (result) {
            binding.content.infoAccessibilityBg.setBackgroundColor(getColor(R.color.success_color))
            binding.content.imageViewAccessibilityService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_done))
            binding.content.textViewAccessibilityServiceInfo.text = getString(R.string.accessibility_start)
        } else {
            binding.content.infoAccessibilityBg.setBackgroundColor(getColor(R.color.warn_color))
            binding.content.imageViewAccessibilityService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_error_white))
            binding.content.textViewAccessibilityServiceInfo.text = getString(R.string.accessibility_no_start)
        }
        return result
    }

    private fun checkShizukuPermission(): Boolean {
        val result = MiFreeform.me?.isRunning?.value!!
        if (result) {
            binding.content.infoShizukuBg.setBackgroundColor(getColor(R.color.success_color))
            binding.content.imageViewShizukuService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_done))
            binding.content.textViewShizukuServiceInfo.text = if (Sui.isSui()) getString(R.string.sui_start) else getString(R.string.shizuku_start)
        } else {
            binding.content.infoShizukuBg.setBackgroundColor(getColor(R.color.warn_color))
            binding.content.imageViewShizukuService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_error_white))
            binding.content.textViewShizukuServiceInfo.text = getString(R.string.shizuku_no_start)
        }
        return result
    }

    private fun checkXposedPermission() {
        val isActive = HookTest.checkXposed()
        if (isActive) {
            binding.content.xposedInfoBg.setBackgroundColor(getColor(R.color.success_color))
            binding.content.imageViewXposedService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_done))
            binding.content.textViewServiceXposedInfo.text = getString(R.string.xposed_start)
            binding.content.textViewServiceXposedInfo.requestFocus()
        }
    }

    private fun checkNotificationPermission() {
        val isActive = PermissionUtils.checkNotificationListenerPermission(this)
        if (isActive) {
            binding.content.infoNotificationBg.setBackgroundColor(getColor(R.color.success_color))
            binding.content.imageViewNotificationService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_done))
            binding.content.textViewNotificationServiceInfo.text = getString(R.string.notification_start)
            binding.content.textViewNotificationServiceInfo.requestFocus()
        } else {
            binding.content.infoNotificationBg.setBackgroundColor(getColor(R.color.warn_color))
            binding.content.imageViewNotificationService.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_error_white))
            binding.content.textViewNotificationServiceInfo.text = getString(R.string.notification_no_start)
            binding.content.textViewNotificationServiceInfo.requestFocus()
        }
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.materialCardView_xposed_info -> {
                MaterialAlertDialogBuilder(this).apply {
                    setTitle(getString(R.string.warn))
                    setMessage(getString(R.string.xposed_permission_intro))
                    setPositiveButton(getString(R.string.done)) {_, _ ->}
                    setCancelable(false)
                    create().show()
                }
            }
            R.id.materialCardView_shizuku_info -> {
                MiFreeform.me?.initShizuku()
                MiFreeform.me?.isRunning?.observe(this) {
                    checkShizukuPermission()
                }
            }
            R.id.materialCardView_accessibility_info -> {
                accessibilityRFAR.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            R.id.materialCardView_overlay_info -> {
                overlayRFAR.launch(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            }
            R.id.materialCardView_notification_info -> {
                notificationRFAR.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }
}