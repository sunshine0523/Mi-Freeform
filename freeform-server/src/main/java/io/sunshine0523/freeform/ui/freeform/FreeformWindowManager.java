package io.sunshine0523.freeform.ui.freeform;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;

import java.util.HashMap;

import io.sunshine0523.freeform.ui.freeform.AppConfig;
import io.sunshine0523.freeform.ui.freeform.FreeformConfig;
import io.sunshine0523.freeform.ui.freeform.FreeformWindow;
import io.sunshine0523.freeform.ui.freeform.UIConfig;

public class FreeformWindowManager {
    static String topWindow = "";
    private static final HashMap<String, FreeformWindow> freeformWindows = new HashMap<>();
    public static void addWindow(
            Handler uiHandler, Context context,
            ComponentName componentName, int userId,
            int width, int height, int densityDpi, float refreshRate,
            boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations,
            String resPkg, String layoutName) {
        AppConfig appConfig = new AppConfig(componentName, userId);
        FreeformConfig freeformConfig = new FreeformConfig(width, height, densityDpi, secure, ownContentOnly, shouldShowSystemDecorations, refreshRate);
        UIConfig uiConfig = new UIConfig(resPkg, layoutName);
        FreeformWindow window = new FreeformWindow(uiHandler, context, appConfig, freeformConfig, uiConfig);
        //if freeform exist, remove old
        FreeformWindow oldWindow = freeformWindows.get(window.getFreeformId());
        if (oldWindow != null) oldWindow.destroy(false);
        freeformWindows.put(window.getFreeformId(), window);
    }

    /**
     * @param freeformId packageName,activityName,userId
     */
    public static void removeWindow(String freeformId) {
        FreeformWindow removedWindow = freeformWindows.remove(freeformId);
        if (removedWindow != null) removedWindow.destroy(false);
    }
}
