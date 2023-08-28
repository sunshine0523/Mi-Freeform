package io.sunshine0523.freeform.service;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;

import io.sunshine0523.freeform.ui.sidebar.SideBarWindow;
import io.sunshine0523.freeform.util.DataHelper;
import io.sunshine0523.freeform.util.Settings;

public class SideBarService {
    private final Context context;
    private final Handler uiHandler;
    private final Settings settings;
    private SideBarWindow sideBarWindow = null;
    SideBarService(Context context, Handler uiHandler, Settings settings) {
        this.context = context;
        this.uiHandler = uiHandler;
        this.settings = settings;

        new Thread(() -> {
            if (settings.getEnableSideBar()) {
                // wait system ui boot
                SystemServiceHolder.waitSystemService("wallpaper");
                sideBarWindow = new SideBarWindow(context, uiHandler);
            }
        }).start();
    }

    public void setSideBarStatus(boolean show) {
        if (null != sideBarWindow) sideBarWindow.destroy();
        if (show) sideBarWindow = new SideBarWindow(context, uiHandler);
        settings.setEnableSideBar(show);
        DataHelper.INSTANCE.saveSettings(settings);
    }
}
