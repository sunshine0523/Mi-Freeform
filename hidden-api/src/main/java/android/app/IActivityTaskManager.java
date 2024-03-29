package android.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IActivityTaskManager extends IInterface {
    void registerTaskStackListener(ITaskStackListener listener) throws RuntimeException;
    void unregisterTaskStackListener(ITaskStackListener listener) throws RuntimeException;
    boolean removeTask(int taskId);
    //For A11-A13
    void moveRootTaskToDisplay(int taskId, int displayId) throws RuntimeException;
    //Only for A10
    void moveStackToDisplay(int stackId, int displayId) throws RuntimeException;

    abstract class Stub extends Binder implements IActivityTaskManager {
        public static IActivityTaskManager asInterface(IBinder binder) {
            throw new RuntimeException("Stub!");
        }
    }
}
