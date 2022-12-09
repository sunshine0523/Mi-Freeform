package com.sunshine.freeform.utils

import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import androidx.annotation.RequiresApi


/**
 * @author sunshine
 * @date 2021/3/22
 * 包相关辅助函数
 */
object PackageUtils {
    
    //判断是否安装了这个应用
    fun hasInstallThisPackage(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    //判断某个用户是否安装了这个应用
    fun hasInstallThisPackageWithUserId(
        packageName: String,
        launcherApps: LauncherApps,
        userHandle: UserHandle
    ): Boolean {
        return try {
            launcherApps.getApplicationInfo(packageName, 0, userHandle)
            true
        } catch (e: Exception) {
            false
        }
    }
}