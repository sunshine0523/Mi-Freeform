package com.sunshine.freeform

import android.content.ComponentName
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.sunshine.freeform.ui.main.RemoteSettings
import io.sunshine0523.freeform.IMiFreeformUIService
import org.lsposed.hiddenapibypass.HiddenApiBypass

object MiFreeformServiceManager {
    private const val TAG = "MiFreeformServiceManager"
    private var iMiFreeformService: IMiFreeformUIService? = null
    private val gson = Gson()

    fun init() {
        try {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val r = HiddenApiBypass.invoke(serviceManager, null, "getService", "mi_freeform") as IBinder
            Log.i(TAG, "mfs $r")
            iMiFreeformService = IMiFreeformUIService.Stub.asInterface(r)
            iMiFreeformService?.ping()
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            e.printStackTrace()
        }
    }

    fun ping(): Boolean {
        return try {
            iMiFreeformService!!.ping()
            true
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            e.printStackTrace()
            false
        }
    }

    fun createDisplay(componentName: ComponentName, userId: Int, width: Int, height: Int, densityDpi: Int) {
        iMiFreeformService?.startAppInFreeform(
            componentName,
            userId,
            1000,
            1400,
            320,
            120.0f,
            true,
            true,
            false,
            "com.sunshine.freeform",
            "view_freeform"
        )
    }

    fun getSetting(): String? {
        return iMiFreeformService?.settings
    }

    fun setSetting(setting: RemoteSettings) {
        iMiFreeformService?.settings = gson.toJson(setting)
    }

    fun removeFreeform(freeformId: String) {
        iMiFreeformService?.removeFreeform(freeformId)
    }
}