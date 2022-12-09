package com.sunshine.freeform.service.notification

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.IIntentSender
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.room.NotificationAppsEntity
import com.sunshine.freeform.systemapi.UserHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method
import java.util.Arrays.stream


/**
 * 监听通知类
 */
class NotificationService : NotificationListenerService(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val scope = MainScope()
    private lateinit var notificationManager: NotificationManager
    private lateinit var sp: SharedPreferences
    private lateinit var viewModel: NotificationViewModel
    private var notificationApps = ArrayList<NotificationAppsEntity>()
    private val notificationAppsPackageName = ArrayList<String>()

    private var enable = false
    /**
     * 服务开启
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sp = application.getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)
        viewModel = NotificationViewModel(this)
        enable = sp.getBoolean("notify_freeform", false)
        getNotificationApps()

        sp.registerOnSharedPreferenceChangeListener(this)
    }

    private var lastNotificationTime = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!enable || sbn?.packageName == "com.sunshine.freeform") return

        if (notificationApps.isNotEmpty() && sbn != null) {
            //如果通知是指定软件发送的
            if (notificationAppsPackageName.contains(sbn.packageName)) {
                //不可清除通知不接收，只接受消息
                if (!sbn.isClearable) return
                //如果和上次时间一样，那么判断为是一条通知，不知道为什么，cancel就会出两条通知，只能这样了
                if (sbn.postTime == lastNotificationTime) return
                //发出自定义通知
                freeformNotification(sbn)
                //清除源应用通知
                cancelNotification(sbn.key)
                lastNotificationTime = sbn.postTime
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when(key) {
            "notify_freeform" -> {
                enable = sp.getBoolean("notify_freeform", false)
            }
            //当所选应用变化时调用
            "notify_freeform_changed" -> {
                getNotificationApps()
            }
        }
    }

    private fun getNotificationApps() {
        scope.launch(Dispatchers.IO) {
            notificationApps = (viewModel.getAllNotificationApps().first() as ArrayList<NotificationAppsEntity>?)!!

            notificationAppsPackageName.clear()
            notificationApps.forEach {
                notificationAppsPackageName.add(it.packageName)
            }
        }
    }

    /**
     * 小窗通知
     */
    @SuppressLint("UseCompatLoadingForDrawables", "UnspecifiedImmutableFlag")
    private fun freeformNotification(sbn: StatusBarNotification) {
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
                getString(R.string.freeform_channel_id),
                NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, NotificationIntentService::class.java)
        intent.putExtra("packageName", sbn.packageName)
        intent.putExtra("userId", UserHandle.getUserId(sbn.user))
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val freeFormButton = Notification.Action.Builder(smallIcon, getString(R.string.open_by_freeform), pendingIntent).build()

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
        if (largeIcon != null) notificationBuilder.setLargeIcon(largeIcon)
        val notification = notificationBuilder.build()
        //点击通知后消失
        notification.flags = Notification.FLAG_AUTO_CANCEL
        notificationManager.notify(2, notification)
    }

//    @RequiresApi(Build.VERSION_CODES.P)
//    @SuppressLint("DiscouragedPrivateApi")
//    private fun getIntent(pendingIntent: PendingIntent?): Intent? {
//        var intent: Intent? = null
//        try {
////            val getIntent: Method = PendingIntent::class.java.getDeclaredMethod("getIntent")
////            getIntent.isAccessible = true
//            val mTargetField = PendingIntent::class.java.getDeclaredField("mTarget")
//            mTargetField.isAccessible = true
//
//            val mTarget = mTargetField.get(pendingIntent)
//            Log.e(TAG, "$mTarget")
//
//            val iActivityManager = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity")))
//
//            intent = iActivityManager?.getIntentForIntentSender(mTarget as IIntentSender)
//        } catch (e: Exception) {
//            Log.e(TAG, "$e")
//        }
//        return intent
//    }

    companion object {
        const val CHANNEL_ID = "CHANNEL_ID_SUNSHINE_FREEFORM_NOTIFICATION"
        private const val TAG = "NotificationService"
    }

}