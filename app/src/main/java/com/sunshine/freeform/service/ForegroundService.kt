package com.sunshine.freeform.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.floating_view.FloatingViewActivity
import com.sunshine.freeform.activity.main.MainActivity
import com.sunshine.freeform.hook.service.MiFreeFormService
import com.sunshine.freeform.utils.PermissionUtils

class ForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "CHANNEL_ID_SUNSHINE_FREEFORM_FOREGROUND"
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val notificationIntent = Intent(this, FloatingViewActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
//        val builder = Notification.Builder(this.applicationContext,
//            CHANNEL_ID
//        )
//        //创建通知渠道
//        val channel = NotificationChannel(
//            CHANNEL_ID,
//                getString(R.string.foreground_notification_name),
//                NotificationManager.IMPORTANCE_LOW
//        )
//        notificationManager.createNotificationChannel(channel)
//
//        builder.setContentIntent(pendingIntent)
//                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
//                .setContentTitle(getString(R.string.foreground_notification_title))
//                .setContentText(getString(R.string.foreground_notification_text))
//                .setSmallIcon(R.drawable.tile_icon)
//                .setWhen(System.currentTimeMillis())
//        val notification = builder.build()
//        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
//
//        startForeground(3, notification)
//
//        if (MiFreeFormService.getClient() == null && PermissionUtils.checkPermission(this)) MainActivity.listener?.onStart()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }
}