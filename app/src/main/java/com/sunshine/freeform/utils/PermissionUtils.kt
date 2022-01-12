package com.sunshine.freeform.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sunshine.freeform.callback.ServiceStateListener
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

/**
 * @author sunshine
 * @date 2021/2/4
 * 权限判断工具类
 */
object PermissionUtils {

    /**
     * 测试sui服务是否运行
     */
    fun checkPermission(code: Int, activity: Activity): Boolean {
        try {
            return if (!Shizuku.isPreV11() && Shizuku.getVersion() >= 11) {
                // Sui and Shizuku >= 11 use self-implemented permission
                when {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                        true
                    }
                    Shizuku.shouldShowRequestPermissionRationale() -> {
                        false
                    }
                    else -> {
                        Shizuku.requestPermission(code)
                        false
                    }
                }
            } else {
                // Shizuku < 11 uses runtime permission
                when {
                    ContextCompat.checkSelfPermission(
                        activity,
                        ShizukuProvider.PERMISSION
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        true
                    }
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        ShizukuProvider.PERMISSION
                    ) -> {
                        false
                    }
                    else -> {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(ShizukuProvider.PERMISSION),
                            code
                        )
                        false
                    }
                }
            }
        } catch (e: Throwable) {

        }
        return false
    }

    fun checkPermission(context: Context): Boolean {
        try {
            return if (!Shizuku.isPreV11() && Shizuku.getVersion() >= 11) {
                // Sui and Shizuku >= 11 use self-implemented permission
                when {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                        true
                    }
                    else -> {
                        false
                    }
                }
            } else {
                // Shizuku < 11 uses runtime permission
                ContextCompat.checkSelfPermission(context, ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Throwable) {

        }
        return false
    }

    //通知使用权是否开启监听
    var notificationStateListener: ServiceStateListener? = null
}