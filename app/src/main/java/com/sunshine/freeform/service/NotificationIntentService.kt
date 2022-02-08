package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import com.sunshine.freeform.MiFreeForm
import com.sunshine.freeform.view.floating.FreeFormView
import java.lang.reflect.Method

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

    @SuppressLint("WrongConstant")
    private fun startFreeForm(targetPackage: String) {
        //关闭通知栏
        collapseStatusBar()
        //点击后清除小窗
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)

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
            FreeFormView(this, command, targetPackage)
            stopSelf()
        } catch (e: PackageManager.NameNotFoundException) {
            stopSelf()
        }
    }

    @SuppressLint("WrongConstant")
    private fun collapseStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MiFreeForm.baseViewModel.getControlService()?.execShell("cmd statusbar collapse")
        } else {
            val service = getSystemService("statusbar") ?: return
            try {
                val clazz = Class.forName("android.app.StatusBarManager")
                val collapse: Method? = clazz.getMethod("collapsePanels")
                collapse?.isAccessible = true
                collapse?.invoke(service)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}