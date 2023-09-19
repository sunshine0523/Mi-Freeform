package com.sunshine.freeform.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager
import android.view.WindowManagerHidden

class SidebarService : Service() {

    private lateinit var viewModel: ServiceViewModel
    private val layoutParams = WindowManagerHidden.LayoutParams()
    private lateinit var windowManager: WindowManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        viewModel = ServiceViewModel(application)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (viewModel.getBooleanSp("sideline", false)) initSideLine()
        return START_STICKY
    }

    /**
     * 启动侧边条
     */
    private fun initSideLine() {
//        val screenWidth = resources.displayMetrics.widthPixels
//        val isLeft = viewModel.getBooleanSp()
//        layoutParams.apply {
//            type = 2026
//            width = 100
//            height = screenHeight / 5
//            x = -screenWidth / 2
//            y = -screenHeight / 6
//            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
//                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
//            privateFlags = WindowManagerHidden.LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_USE_BLAST or WindowManagerHidden.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY
//            format = PixelFormat.RGBA_8888
//        }
    }
}