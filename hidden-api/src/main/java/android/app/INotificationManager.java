package android.app;

import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.service.notification.INotificationListener;

public interface INotificationManager extends IInterface {
    void registerListener(INotificationListener listener, ComponentName component, int userid);
    void unregisterListener(INotificationListener listener, int userid);
    void cancelNotificationWithTag(String pkg, String opPkg, String tag, int id, int userId);
    void cancelNotificationsFromListener(INotificationListener token, String[] keys);
    abstract class Stub extends Binder implements INotificationManager {
        public static INotificationManager asInterface(IBinder binder) {
            throw new RuntimeException("Stub!");
        }
    }
}
