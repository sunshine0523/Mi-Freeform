package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IWindowManager extends IInterface {
    int watchRotation(IRotationWatcher watcher, int displayId);

    void removeRotationWatcher(IRotationWatcher watcher);

    //Only support A12+
    void setDisplayImePolicy(int displayId, int imePolicy);

    void freezeDisplayRotation(int displayId, int rotation);

    abstract class Stub extends Binder implements IWindowManager {
        public static IWindowManager asInterface(IBinder binder) {
            throw new RuntimeException("Stub!");
        }
    }
}
