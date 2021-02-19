package com.sunshine.freeform.utils

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.sunshine.freeform.callback.ServiceStateListener

/**
 * @author sunshine
 * @date 2021/2/4
 * 权限判断工具类
 */
object PermissionUtils {
    //无障碍判断
    fun hasAccessibility(context: Context): Boolean {
        val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
        val settingValue = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (settingValue != null) {
            mStringColonSplitter.setString(settingValue)
            while (mStringColonSplitter.hasNext()) {
                val accessibilityService = mStringColonSplitter.next()
                if (accessibilityService.equals("${context.packageName}/${context.packageName}.service.FreeFormAccessibilityService", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    //无障碍是否开启监听
    var accessibilityStateListener: ServiceStateListener? = null

    //通知使用权是否开启监听
    var notificationStateListener: ServiceStateListener? = null
}