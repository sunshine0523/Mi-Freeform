// IControlService.aidl
package com.sunshine.freeform;

// Declare any non-default types here with import statements
import com.sunshine.freeform.bean.MotionEventBean;
import com.sunshine.freeform.callback.IOnRotationChangedListener;
import android.view.Surface;
import android.os.IInterface;

interface IControlService {
    //初始化远程服务
    boolean init();
    //点击指定屏幕的返回键
    void pressBack(int displayId);
    //触摸指定屏幕
    void touch(in MotionEventBean motionEventBean);
    //将屏幕显示内容移动到系统屏幕上
    boolean moveStack(int displayId);
    //获取屏幕方向
    int getRotation();
    //屏幕方向监听器
    boolean initRotationWatcher(in IOnRotationChangedListener callback);
    boolean execShell(String command);
}