package com.sunshine.freeform.utils

import android.app.ActivityManager
import android.content.Context

/**
 * @date 2021/2/1
 * 服务相关工具类
 */
object ServiceUtils {
    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param mContext
     * @param serviceName 是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）
     * @return true代表正在运行，false代表服务没有正在运行
     */
    fun isServiceWork(mContext: Context, serviceName: String): Boolean {
        var isWork = false
        val myAM = mContext
                .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val myList: List<ActivityManager.RunningServiceInfo> = myAM.getRunningServices(40)
        if (myList.isEmpty()) {
            return false
        }
        for (i in myList.indices) {
            val mName: String = myList[i].service.className
            myList[i].service.className
            if (mName == serviceName) {
                isWork = true
                break
            }
        }
        return isWork
    }
}