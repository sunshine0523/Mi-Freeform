package android.service.notification;

import android.os.IInterface;

public interface IStatusBarNotificationHolder extends IInterface {
    /** Fetch the held StatusBarNotification. This method should only be called once per Holder */
    StatusBarNotification get();
}
