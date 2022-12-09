package com.sunshine.freeform.systemapi

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.IInterface
import java.lang.reflect.Method

@SuppressLint("DiscouragedPrivateApi")
class ServiceManager {
    private var inputManager: InputManager? = null
    private var windowManager: WindowManager? = null

    private val getServiceMethod: Method? = try {
        Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun getService(service: String, type: String): IInterface? {
        return try {
            val binder = getServiceMethod!!.invoke(null, service) as IBinder
            val asInterfaceMethod = Class.forName("$type\$Stub").getMethod(
                "asInterface",
                IBinder::class.java
            )
            asInterfaceMethod.invoke(null, binder) as IInterface
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getInputManager(): InputManager? {
        if (inputManager == null) {
            val inputManagerServer = getService("input", "android.hardware.input.IInputManager")
            if (inputManagerServer == null) return null
            else inputManager = InputManager(inputManagerServer)
        }
        return inputManager
    }

    fun getWindowManager(): WindowManager? {
        if (windowManager == null) {
            windowManager = WindowManager(getService("window", "android.view.IWindowManager")!!)
        }
        return windowManager
    }


}