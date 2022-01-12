package com.sunshine.freeform

import android.annotation.SuppressLint


/**
 * @author sunshine
 * @date 2021/2/10
 * 服务类
 * 用于监听触摸事件
 */
object Server {
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @JvmStatic
    fun main(args: Array<String>) {
        SocketServer()

//        Looper.prepareMainLooper()
//        val activityThread =
//            Class.forName("android.app.ActivityThread")
//        val systemMain: Method = activityThread.getDeclaredMethod("systemMain")
//
//        val `object`: Any = systemMain.invoke(null)
//
//        val ContextImpl = Class.forName("android.app.ContextImpl")
//        val createSystemContext: Method =
//            ContextImpl.getDeclaredMethod("createSystemContext", activityThread)
//        createSystemContext.setAccessible(true)
//        val contextInstace: Context = createSystemContext.invoke(null, `object`) as Context
//
//        val context: Context = contextInstace.createPackageContext(
//            "com.sunshine.freeform",
//            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
//        )

//        try {
//            val view = LinearLayout(contextInstace)
//
//            val windowManager = contextInstace.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//            val layoutParams = WindowManager.LayoutParams()
//            layoutParams.flags = 2026
//            layoutParams.width = 100
//            layoutParams.height = 100
//            windowManager.addView(LinearLayout(contextInstace), layoutParams)
//        }catch (e: Exception) {
//            println(e)
//        }

    }
}