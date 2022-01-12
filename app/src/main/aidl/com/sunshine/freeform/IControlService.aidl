// IControlService.aidl
package com.sunshine.freeform;

// Declare any non-default types here with import statements
import com.sunshine.freeform.bean.MotionEventBean;
import com.sunshine.freeform.callback.IOnRotationChangedListener;
import android.view.Surface;
import android.os.IInterface;

interface IControlService {
    boolean init();
    boolean startActivity(String command);
    void pressBack(int displayId);
    void touch(in MotionEventBean motionEventBean);
    boolean moveStack(int displayId);
    int getRotation();
    boolean initRotationWatcher(in IOnRotationChangedListener callback);
    String test(int displayId);
    void setDisplaySurface(in Surface surface);
    String test2();
}