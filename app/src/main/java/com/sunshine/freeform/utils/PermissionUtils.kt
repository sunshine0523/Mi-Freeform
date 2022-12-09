package com.sunshine.freeform.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import androidx.annotation.RequiresApi
import com.sunshine.freeform.service.KeepAliveService

/**
 * @date 2022/8/26
 * @author sunshine0523
 */
object PermissionUtils {

    fun checkNotificationListenerPermission(context: Context): Boolean {
        var enable = false
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (flat != null) {
            enable = flat.contains(context.packageName)
        }
        return enable
    }

    fun checkOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilitySettingsOn(context: Context): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/com.sunshine.freeform.service.KeepAliveService"
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: SettingNotFoundException) {
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}