package com.sunshine.freeform.service

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sunshine.freeform.utils.PermissionUtils

class NotificationService : NotificationListenerService() {

    /**
     * 服务开启
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        if (PermissionUtils.notificationStateListener != null) {
            PermissionUtils.notificationStateListener!!.onStart()
        }
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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}