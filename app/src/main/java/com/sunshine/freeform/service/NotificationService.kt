package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import com.sunshine.freeform.R
import com.sunshine.freeform.room.NotificationAppsEntity
import com.sunshine.freeform.utils.PermissionUtils

/**
 * 监听通知类
 */
class NotificationService : NotificationListenerService() {

    companion object {
        var notificationApps: List<NotificationAppsEntity>? = null
        var compatibleApps: List<String>? = null
        const val CHANNEL_ID = "CHANNEL_ID_SUNSHINE_FREEFORM_NOTIFICATION"
        //仅在横屏启动
        var onlyEnableLandscape = false
    }

    private var notificationManager: NotificationManager? = null
    private var sp: SharedPreferences? = null
    private var orientation = Configuration.ORIENTATION_UNDEFINED

    /**
     * 服务开启
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        if (PermissionUtils.notificationStateListener != null) {
            PermissionUtils.notificationStateListener!!.onStart()
        }
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
        orientation = resources.configuration.orientation
        onlyEnableLandscape = sp?.getBoolean("switch_preference_only_enable_landscape", false)!!
    }

    /**
     * 服务关闭
     */
    override fun onDestroy() {
        super.onDestroy()
        if (PermissionUtils.notificationStateListener != null) {
            PermissionUtils.notificationStateListener!!.onStop()
        }
    }

    private var lastNotificationTime = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!sp?.getBoolean("switch_notify", false)!!) return
        if (sbn?.packageName == "com.sunshine.freeform") return
        if (onlyEnableLandscape && orientation != Configuration.ORIENTATION_LANDSCAPE) return
        if (!notificationApps.isNullOrEmpty() && sbn != null && notificationManager != null) {
            //如果通知是指定软件发送的
            notificationApps?.forEach {
                if (it.packageName == sbn.packageName) {
                    //不可清除通知不接收，只接受消息
                    if (!sbn.isClearable) return
                    //如果和上次时间一样，那么判断为是一条通知，不知道为什么，cancel就会出两条通知，只能这样了
                    if (sbn.postTime == lastNotificationTime) return
                    bubbleNotification(sbn)
                    cancelNotification(sbn.key)
                    lastNotificationTime = sbn.postTime
                }
            }

        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        //super.onNotificationRemoved(sbn)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientation = resources.configuration.orientation
    }

    /**
     * 气泡通知
     * 应该指定一个默认显示的时间，时间过了就不显示了
     * 并且不能显示过多的气泡
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun bubbleNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification?.extras
        val title = extras?.getString(Notification.EXTRA_TITLE)
        val text = extras?.getString(Notification.EXTRA_TEXT)
        val smallIcon: Icon = sbn.notification.smallIcon
        val largeIcon: Icon? = try {
            sbn.notification.getLargeIcon()
        }catch (e: Exception) {
            null
        }
        //创建通知渠道
        val channel = NotificationChannel(
            CHANNEL_ID,
                "小窗应用通知",
                NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager!!.createNotificationChannel(channel)

        val intent = Intent(this, NotificationIntentService::class.java)
        intent.putExtra("package", sbn.packageName)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val freeFormButton = Notification.Action.Builder(smallIcon, "小窗打开", pendingIntent).build()

        //设置通知
        val notificationBuilder = Notification.Builder(this,
            CHANNEL_ID
        )
            .setContentTitle(title)
            .setContentText(text)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(smallIcon)
            .setAutoCancel(true)
            .setContentIntent(sbn.notification.contentIntent)
            .addAction(freeFormButton)
            .setCustomHeadsUpContentView(setFullScreenRemoteViews("$title $text", pendingIntent))
        if (largeIcon != null) notificationBuilder.setLargeIcon(largeIcon)
        val notification = notificationBuilder.build()
        //点击通知后消失
        notification.flags = Notification.FLAG_AUTO_CANCEL
        notificationManager!!.notify(2, notification)
    }

    private fun setFullScreenRemoteViews(notificationText: String, pendingIntent: PendingIntent): RemoteViews {
        val remoteViews = RemoteViews(packageName, R.layout.view_notification)
        remoteViews.setTextViewText(R.id.textView_notificatin, notificationText)
        remoteViews.setOnClickPendingIntent(R.id.button_notification, pendingIntent)

        return remoteViews
    }

}