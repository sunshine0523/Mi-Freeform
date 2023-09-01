package io.sunshine0523.freeform.service;

import android.app.IActivityTaskManager;
import android.os.ServiceManager;
import android.util.Log;
import android.view.IWindowManager;

import com.android.server.am.ActivityManagerService;
import com.android.server.input.InputManagerService;

import io.sunshine0523.freeform.util.MLog;

public class SystemServiceHolder {

    private static final String TAG = "Mi-Freeform/SystemServiceHolder";

    static InputManagerService inputManagerService;
    public static IActivityTaskManager activityTaskManager;
    public static IWindowManager windowManager;

    static void init(ServiceCallback callback) {
        new Thread(() -> {
            waitSystemService("activity_task");
            waitSystemService("input");
            waitSystemService("window");
            activityTaskManager = IActivityTaskManager.Stub.asInterface(ServiceManager.getService("activity_task"));
            inputManagerService = (InputManagerService) ServiceManager.getService("input");
            windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            callback.allAdded();
        }).start();
    }

    public static void waitSystemService(String name) {
        try {
            while (null == ServiceManager.getService(name)) {
                Thread.sleep(1000);
                MLog.i(TAG, name + " not start, wait 1s");
            }
        } catch (Exception ignored) { }
    }

    interface ServiceCallback {
        void allAdded();
    }
}
