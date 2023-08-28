package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IRotationWatcher extends IInterface {
    void onRotationChanged(int rotation) throws RuntimeException;

    abstract class Stub extends Binder implements IRotationWatcher{
        @Override
        public IBinder asBinder() {
            throw new RuntimeException("Stub!");
        }
    }
}
