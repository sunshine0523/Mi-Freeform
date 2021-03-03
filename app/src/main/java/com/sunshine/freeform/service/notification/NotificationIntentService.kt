package com.sunshine.freeform.service.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import com.sunshine.freeform.service.floating.FreeFormWindow

/**
 * 用于开启目标程序
 */
class NotificationIntentService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetPackage: String? = intent?.getStringExtra("package")
        if (targetPackage.isNullOrBlank()) stopSelf()
        else startFreeForm(targetPackage)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startFreeForm(targetPackage: String) {
        //如果触控服务没有开启开启触控服务

        val sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
        val model = sp.getInt("freeform_display_model", 1)
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfo = packageManager.queryIntentActivities(intent, 0)

        try {
            var activityName = ""

            resolveInfo.forEach {
                if (it.activityInfo.applicationInfo.packageName == targetPackage) {
                    activityName = it.activityInfo.name
                }
            }
            val command = "am start -n ${targetPackage}/${activityName} --display "
            FreeFormWindow(this, command, targetPackage)

//            when (model) {
//                3 -> {
//                    FreeFormTextureWindow(this, command, packageName)
//                }
//                2 -> {
//                    FreeFormMediaCodecWindow(this, command, packageName)
//                }
//                else -> {
//                    FreeFormImageReaderWindow(this, command, packageName)
//                }
//            }
            stopSelf()
        } catch (e: PackageManager.NameNotFoundException) {
            stopSelf()
        }
    }
}