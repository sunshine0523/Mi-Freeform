package com.sunshine.freeform

import android.app.PendingIntent
import android.os.Build
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import com.google.gson.Gson
import com.sunshine.freeform.ui.main.RemoteSettings
import io.sunshine0523.freeform.IMiFreeformUIService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.Date

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

    fun createWindow(packageName: String, activityName: String, userId: Int, width: Int, height: Int, densityDpi: Int) {
        iMiFreeformService?.startAppInFreeform(
            packageName,
            activityName,
            userId,
            null,
            width,
            height,
            densityDpi,
            120.0f,
            false,
            true,
            false,
            "com.sunshine.freeform",
            "view_freeform"
        )
    }

    fun createWindow(pendingIntent: PendingIntent?, width: Int, height: Int, densityDpi: Int) {
        iMiFreeformService?.startAppInFreeform(
            pendingIntent?.creatorPackage?:"pendingIntentCreatorPackage",
            "unknownActivity-${Date().time}",
            -100,
            pendingIntent,
            width,
            height,
            densityDpi,
            120.0f,
            false,
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

    fun cancelNotification(key: String?) {
        iMiFreeformService?.cancelNotification(key)
    }
}