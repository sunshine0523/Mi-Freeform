package com.sunshine.freeform.activity.permission

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.OrientationEventListener
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sunshine.freeform.R
import com.sunshine.freeform.callback.ServiceStateListener
import com.sunshine.freeform.utils.PermissionUtils
import com.sunshine.freeform.utils.ShellUtils
import com.sunshine.freeform.utils.TagUtils
import kotlinx.android.synthetic.main.activity_permission.*


class PermissionActivity : AppCompatActivity(), View.OnClickListener {

    //当前已经获取权限的数量，5为全部获取成功
    private var hasPermissionCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        serviceListener()
        hasDrawOverlays()
        hasNotificationListener()
        hasGetUsageStats()
        hasAccessibility()
        hasRoot()
    }

    //设置服务监听
    private fun serviceListener() {
        //无障碍权限监听，没找到回调，先这样设置
        PermissionUtils.accessibilityStateListener = object : ServiceStateListener {
            override fun onStart() {
                button_accessibility.isClickable = false
                button_accessibility.text = getString(R.string.accessibility_permission_granted)
                hasPermissionCount++
            }

            override fun onStop() {
                button_accessibility.isCheckable = true
                button_accessibility.text = getString(R.string.accessibility_permission_denied)
                button_accessibility.setOnClickListener(this@PermissionActivity)
                hasPermissionCount--
            }

        }

        //通知使用权监听
        PermissionUtils.notificationStateListener = object : ServiceStateListener {
            override fun onStart() {
                button_notification_listeners.isClickable = false
                button_notification_listeners.text = getString(R.string.notification_listeners_permission_granted)
                hasPermissionCount++
            }

            override fun onStop() {
                button_notification_listeners.isCheckable = true
                button_notification_listeners.text = getString(R.string.notification_listeners_permission_denied)
                button_notification_listeners.setOnClickListener(this@PermissionActivity)
                hasPermissionCount--
            }

        }
    }

    //悬浮窗权限
    private fun hasDrawOverlays() {
        try {
            if (Settings.canDrawOverlays(this)) {
                button_draw_overlay.isClickable = false
                button_draw_overlay.text = getString(R.string.draw_overlay_permission_granted)
                hasPermissionCount++
            } else {
                button_draw_overlay.isCheckable = true
                button_draw_overlay.text = getString(R.string.draw_overlay_permission_denied)
                button_draw_overlay.setOnClickListener(this)
                //hasPermissionCount--
            }
        }catch (e: Exception) {
            button_draw_overlay.isClickable = false
            button_draw_overlay.text = getString(R.string.cannot_granted_overlay)
            hasPermissionCount++
        }

    }

    //通知使用权
    private fun hasNotificationListener() {
        try {
            if (Settings.Secure.getString(contentResolver, "enabled_notification_listeners").contains(packageName)) {
                button_notification_listeners.isClickable = false
                button_notification_listeners.text = getString(R.string.notification_listeners_permission_granted)
                hasPermissionCount++
            } else {
                button_notification_listeners.isCheckable = true
                button_notification_listeners.text = getString(R.string.notification_listeners_permission_denied)
                button_notification_listeners.setOnClickListener(this)
                //hasPermissionCount--
            }
        }catch (e: Exception) {
            //无法判断是否有权限
            button_notification_listeners.isCheckable = false
            button_notification_listeners.text = getString(R.string.cannot_granted_notification)
            hasPermissionCount++
        }

    }

    //使用情况
    private fun hasGetUsageStats() {
        try {
            if ((getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager).checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName
                ) == AppOpsManager.MODE_ALLOWED) {
                button_get_usage_stats.isClickable = false
                button_get_usage_stats.text = getString(R.string.get_usage_stats_permission_granted)
                hasPermissionCount++
            } else {
                button_get_usage_stats.isCheckable = true
                button_get_usage_stats.text = getString(R.string.get_usage_stats_permission_denied)
                button_get_usage_stats.setOnClickListener(this)
                //hasPermissionCount--
            }
        }catch (e: Exception) {
            button_get_usage_stats.isClickable = false
            button_get_usage_stats.text = getString(R.string.cannot_granted_usage)
            hasPermissionCount++
        }

    }

    //无障碍
    private fun hasAccessibility() {
        if (PermissionUtils.hasAccessibility(this)) {
            button_accessibility.isClickable = false
            button_accessibility.text = getString(R.string.accessibility_permission_granted)
            hasPermissionCount++
        } else {
            button_accessibility.isCheckable = true
            button_accessibility.text = getString(R.string.accessibility_permission_denied)
            button_accessibility.setOnClickListener(this)
            //hasPermissionCount--
        }
    }

    //root
    private fun hasRoot() {
        if (ShellUtils.checkRootPermission()) {
            button_root.isCheckable = false
            button_root.text = getString(R.string.root_permission_granted)
            hasPermissionCount++
        } else {
            button_root.isCheckable = true
            button_root.text = getString(R.string.root_permission_denied)
            button_root.setOnClickListener(this)
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.button_draw_overlay -> {
                startActivityForResult(
                        Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                        ), TagUtils.DRAW_OVERLAYS_PERMISSION
                )
            }
            R.id.button_notification_listeners -> {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            R.id.button_get_usage_stats -> {
                startActivityForResult(
                        Intent(
                                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                                Uri.parse("package:$packageName")
                        ), TagUtils.GET_USAGE_STATS_PERMISSION
                )
            }
            R.id.button_accessibility -> {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            R.id.button_root -> {
                hasRoot()
            }
        }
    }

    /**
     * 返回键监听
     */
    override fun onBackPressed() {
        //先执行再调用super
        if (hasPermissionCount < 5) setResult(RESULT_CANCELED)
        else setResult(RESULT_OK)
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            TagUtils.DRAW_OVERLAYS_PERMISSION -> {
                hasDrawOverlays()
            }
            TagUtils.GET_USAGE_STATS_PERMISSION -> {
                hasGetUsageStats()
            }
        }
    }
}