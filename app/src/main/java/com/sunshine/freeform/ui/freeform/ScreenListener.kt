package com.sunshine.freeform.ui.freeform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * 创建一个监听器类 监听android锁屏与解锁事件
 */
class ScreenListener(private val mContext: Context) {
    private val mScreenReceiver: ScreenBroadcastReceiver
    private var mScreenStateListener: ScreenStateListener? = null

    /**
     * screen状态广播接收者
     */
    private inner class ScreenBroadcastReceiver : BroadcastReceiver() {
        private var action: String? = null
        override fun onReceive(context: Context, intent: Intent) {
            action = intent.action
            if (Intent.ACTION_SCREEN_ON == action) { // 开屏
                mScreenStateListener!!.onScreenOn()
            } else if (Intent.ACTION_SCREEN_OFF == action) { // 锁屏
                mScreenStateListener!!.onScreenOff()
            } else if (Intent.ACTION_USER_PRESENT == action) { // 解锁
                mScreenStateListener!!.onUserPresent()
            }
        }
    }

    /**
     * 开始监听screen状态
     *
     * @param listener
     */
    fun begin(listener: ScreenStateListener?) {
        mScreenStateListener = listener
        registerListener()
    }

    /**
     * 停止screen状态监听
     */
    fun unregisterListener() {
        mContext.unregisterReceiver(mScreenReceiver)
    }

    /**
     * 启动screen状态广播接收器
     */
    private fun registerListener() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        mContext.registerReceiver(mScreenReceiver, filter)
    }

    interface ScreenStateListener {
        // 返回给调用者屏幕状态信息
        fun onScreenOn()
        fun onScreenOff()
        fun onUserPresent()
    }

    init {
        mScreenReceiver = ScreenBroadcastReceiver()
    }
}