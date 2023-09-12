package android.app;

import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

/**
 * Only For Android 8.1-Android 9
 */
public interface IActivityManager extends IInterface {
    void registerTaskStackListener(ITaskStackListener listener) throws RuntimeException;
    void unregisterTaskStackListener(ITaskStackListener listener) throws RuntimeException;
    boolean removeTask(int taskId) throws RuntimeException;
    void moveStackToDisplay(int stackId, int displayId) throws RuntimeException;

    int sendIntentSender(IIntentSender target, IBinder whitelistToken, int code,
                         Intent intent, String resolvedType, IIntentReceiver finishedReceiver,
                         String requiredPermission, Bundle options);

    abstract class Stub extends Binder implements IActivityManager {
        public static IActivityManager asInterface(IBinder binder) {
            throw new RuntimeException("Stub!");
        }
    }
}
