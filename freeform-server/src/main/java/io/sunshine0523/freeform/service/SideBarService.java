package io.sunshine0523.freeform.service;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import io.sunshine0523.freeform.ui.sidebar.SideBarWindow;
import io.sunshine0523.freeform.util.DataChangeListener;
import io.sunshine0523.freeform.util.DataHelper;
import io.sunshine0523.freeform.util.Settings;

public class SideBarService implements DataChangeListener {
    private final Context context;
    private final Handler uiHandler;
    private Settings settings;
    private SideBarWindow sideBarWindow = null;
    SideBarService(Context context, Handler uiHandler, Settings settings) {
        this.context = context;
        this.uiHandler = uiHandler;
        this.settings = settings;

//        new Thread(() -> {
//            if (settings.getEnableSideBar()) {
//                // wait system ui boot
//                SystemServiceHolder.waitSystemService("wallpaper");
//                sideBarWindow = new SideBarWindow(context, uiHandler);
//            }
//        }).start();
    }

    @Override
    public void onChanged() {
//        settings = DataHelper.INSTANCE.getSettings();
//        setSideBarStatus(settings.getEnableSideBar());
    }

    public void setSideBarStatus(boolean show) {
        if (null != sideBarWindow) sideBarWindow.destroy();
        if (show) sideBarWindow = new SideBarWindow(context, uiHandler);
    }
}
