package com.sunshine.freeform

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.content.Context
import android.os.Looper

/**
 * @author sunshine
 * @date 2022/1/29
 * WARNING:THIS CLASS NOT USED NOW!
 */
object Server {
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @JvmStatic
    fun main(args: Array<String>) {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper()
        }
        val service: IControlService?
        val systemContext: Context = ActivityThread.systemMain().systemContext

        val serverClass = systemContext.classLoader.loadClass("com.sunshine.freeform.ControlService")
        service = serverClass.newInstance() as IControlService
        service.init()
        Looper.loop()
    }
}