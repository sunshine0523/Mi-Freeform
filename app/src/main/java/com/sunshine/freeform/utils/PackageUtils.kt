package com.sunshine.freeform.utils

import android.content.pm.PackageManager

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
}