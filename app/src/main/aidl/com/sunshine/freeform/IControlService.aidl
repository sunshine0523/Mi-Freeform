// IControlService.aidl
package com.sunshine.freeform;

// Declare any non-default types here with import statements
import com.sunshine.freeform.bean.MotionEventBean;
import com.sunshine.freeform.callback.IOnRotationChangedListener;
import com.sunshine.freeform.callback.IAppRunningListener;
import android.view.MotionEvent;

interface IControlService {
    boolean init();
    void pressBack(int displayId);
    void touch(in MotionEventBean motionEventBean);
    boolean moveStack(int displayId);
    boolean execShell(String command, boolean useRoot);
}