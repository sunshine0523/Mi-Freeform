package com.sunshine.freeform

import android.content.ComponentName
import android.os.Build
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.sunshine.freeform.ui.main.RemoteSettings
import io.sunshine0523.freeform.IMiFreeformUIService
import org.lsposed.hiddenapibypass.HiddenApiBypass

object MiFreeformServiceManager {
    private const val TAG = "MiFreeformServiceManager"
    private var iMiFreeformService: IMiFreeformUIService? = null
    private val gson = Gson()

    fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
        } else {
            try {
                val r = ServiceManager.getService("mi_freeform")
                Log.i(TAG, "mfs $r")
                iMiFreeformService = IMiFreeformUIService.Stub.asInterface(r)
                iMiFreeformService?.ping()
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                e.printStackTrace()
            }
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
        Log.i(TAG, "$width $height $densityDpi")
        iMiFreeformService?.startAppInFreeform(
            componentName,
            userId,
            width,
            height,
            densityDpi,
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

    fun getLog(): String {
        return iMiFreeformService?.log ?: "Maybe Mi-Freeform can`t link mi_freeform service. You can get log at /data/system/mi_freeform/log.log"
    }

    fun clearLog() {
        iMiFreeformService?.clearLog()
    }

    fun collapseStatusBar() {
        iMiFreeformService?.collapseStatusBar()
    }
}