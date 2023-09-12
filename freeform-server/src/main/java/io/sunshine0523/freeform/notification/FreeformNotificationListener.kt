package io.sunshine0523.freeform.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.UserHandle
import android.service.notification.INotificationListener
import android.service.notification.IStatusBarNotificationHolder
import android.service.notification.NotificationRankingUpdate
import android.service.notification.NotificationStats
import android.system.Os
import android.util.Log
import io.sunshine0523.freeform.util.DataChangeListener
import io.sunshine0523.freeform.util.DataHelper
import io.sunshine0523.freeform.util.MLog
import java.util.Arrays

/**
 * @author KindBrave
 * @since 2023/9/9
 */
class FreeformNotificationListener(
    private val userContext: Context,
    private val notificationManager: NotificationManager,
    private val handler: Handler
) : INotificationListener.Stub(), DataChangeListener {

    private var settings = DataHelper.getSettings()
    private val freeformTextId = userContext.resources.getIdentifier("notification_freeform", "string", "com.sunshine.freeform")
    private val freeformText = userContext.resources.getString(freeformTextId)

    companion object {
        private const val TAG = "Mi-Freeform/NotificationListener"
    }

    override fun onListenerConnected(update: NotificationRankingUpdate?) {

    }

    override fun onNotificationPosted(
        notificationHolder: IStatusBarNotificationHolder?,
        update: NotificationRankingUpdate?
    ) {
        if (settings.notification.not()) return
        val sbn = notificationHolder?.get()
        if (sbn != null) {
            val notification = sbn.notification
            var actions = notification.actions
            // if actions has mi-freeform, return
            actions?.forEach {
                if (it.title == freeformText) return
            }
            val pendingIntent =
                (if (sbn.notification.contentIntent != null) sbn.notification.contentIntent else sbn.notification.fullScreenIntent)
                    ?: return
            handler.post {
                try {
                    val activityCls = userContext.classLoader.loadClass("com.sunshine.freeform.ui.freeform.FreeformActivity")
                    val intent = Intent(userContext, activityCls).apply {
                        putExtra("key", sbn.key)
                        putExtra("isClearable", sbn.isClearable)
                        putExtra("pendingIntent", pendingIntent)
                    }
                    val freeformPendingIntent = PendingIntent.getActivity(userContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    val freeformAction = Notification.Action.Builder(sbn.notification.smallIcon, freeformText, freeformPendingIntent).build()
                    if (actions == null) {
                        actions = arrayOf(freeformAction)
                    }
                    // 3 is max action size
                    else if (actions.size < 3){
                        actions = Arrays.copyOf(actions, actions.size + 1)
                        actions[actions.size - 1] = freeformAction
                    }
                    notification.actions = actions
                    notificationManager.cancel(sbn.id)
                    notificationManager.notify(sbn.id, notification)
                    Log.i(TAG, "${sbn.notification}")
                } catch (e: Exception) {
                    MLog.e(TAG, "$e")
                }

            }
        }

    }

    override fun onStatusBarIconsBehaviorChanged(hideSilentStatusIcons: Boolean) {

    }

    override fun onNotificationRemoved(
        notificationHolder: IStatusBarNotificationHolder?,
        update: NotificationRankingUpdate?,
        stats: NotificationStats?,
        reason: Int
    ) {

    }

    override fun onNotificationRankingUpdate(update: NotificationRankingUpdate?) {

    }

    override fun onListenerHintsChanged(hints: Int) {

    }

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {

    }

    override fun onNotificationChannelModification(
        pkgName: String?,
        user: UserHandle?,
        channel: NotificationChannel?,
        modificationType: Int
    ) {

    }

    override fun onNotificationChannelGroupModification(
        pkgName: String?,
        user: UserHandle?,
        group: NotificationChannelGroup?,
        modificationType: Int
    ) {

    }

    override fun onNotificationEnqueuedWithChannel(
        notificationHolder: IStatusBarNotificationHolder?,
        channel: NotificationChannel?,
        update: NotificationRankingUpdate?
    ) {

    }

    override fun onNotificationSnoozedUntilContext(
        notificationHolder: IStatusBarNotificationHolder?,
        snoozeCriterionId: String?
    ) {

    }

    override fun onNotificationsSeen(keys: MutableList<String>?) {

    }

    override fun onPanelRevealed(items: Int) {

    }

    override fun onPanelHidden() {

    }

    override fun onNotificationVisibilityChanged(key: String?, isVisible: Boolean) {

    }

    override fun onNotificationExpansionChanged(
        key: String?,
        userAction: Boolean,
        expanded: Boolean
    ) {

    }

    override fun onNotificationDirectReply(key: String?) {

    }

    override fun onSuggestedReplySent(key: String?, reply: CharSequence?, source: Int) {

    }

    override fun onActionClicked(key: String?, action: Notification.Action?, source: Int) {

    }

    override fun onNotificationClicked(key: String?) {

    }

    override fun onAllowedAdjustmentsChanged() {

    }

    override fun onNotificationFeedbackReceived(
        key: String?,
        update: NotificationRankingUpdate?,
        feedback: Bundle?
    ) {

    }

    override fun onChanged() {
        settings = DataHelper.getSettings()
    }
}