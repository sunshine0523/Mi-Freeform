package com.sunshine.freeform.service.core

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 程序后台核心服务
 * 其他服务可能会依赖此服务
 */
class CoreService : Service() {

    companion object {
        //悬浮窗应用，因为service无法获取room，所以采用这个方式
        var floatingApps: List<String>? = null
        var isRunning = false
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }
}