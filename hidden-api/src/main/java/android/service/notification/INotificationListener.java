package android.service.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.UserHandle;

import java.util.List;

public interface INotificationListener extends IInterface {
    // listeners and assistant
    void onListenerConnected(NotificationRankingUpdate update);
    void onNotificationPosted(IStatusBarNotificationHolder notificationHolder,
                              NotificationRankingUpdate update);
    void onStatusBarIconsBehaviorChanged(boolean hideSilentStatusIcons);
    // stats only for assistant
    void onNotificationRemoved(IStatusBarNotificationHolder notificationHolder,
                               NotificationRankingUpdate update, NotificationStats stats, int reason);
    void onNotificationRankingUpdate(NotificationRankingUpdate update);
    void onListenerHintsChanged(int hints);
    void onInterruptionFilterChanged(int interruptionFilter);

    // companion device managers and assistants only
    void onNotificationChannelModification(String pkgName, UserHandle user, NotificationChannel channel, int modificationType);
    void onNotificationChannelGroupModification(String pkgName, UserHandle user, NotificationChannelGroup group, int modificationType);

    // assistants only
    void onNotificationEnqueuedWithChannel(IStatusBarNotificationHolder notificationHolder, NotificationChannel channel, NotificationRankingUpdate update);
    void onNotificationSnoozedUntilContext(IStatusBarNotificationHolder notificationHolder, String snoozeCriterionId);
    void onNotificationsSeen(List<String> keys);
    void onPanelRevealed(int items);
    void onPanelHidden();
    void onNotificationVisibilityChanged(String key, boolean isVisible);
    void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded);
    void onNotificationDirectReply(String key);
    void onSuggestedReplySent(String key, CharSequence reply, int source);
    void onActionClicked(String key, Notification.Action action, int source);
    void onNotificationClicked(String key);
    void onAllowedAdjustmentsChanged();
    void onNotificationFeedbackReceived(String key, NotificationRankingUpdate update, Bundle feedback);

    abstract class Stub extends Binder implements INotificationListener {
        @Override
        public IBinder asBinder() {
            throw new RuntimeException("Stub!");
        }
    }
}
