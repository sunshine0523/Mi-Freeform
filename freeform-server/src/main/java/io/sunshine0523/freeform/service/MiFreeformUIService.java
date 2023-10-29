package io.sunshine0523.freeform.service;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static android.content.Context.CONTEXT_INCLUDE_CODE;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.view.Surface;

import com.android.server.display.MiFreeformDisplayAdapter;

import java.util.Map;

import io.sunshine0523.freeform.IMiFreeformDisplayCallback;
import io.sunshine0523.freeform.IMiFreeformUIService;
import io.sunshine0523.freeform.notification.FreeformNotificationListener;
import io.sunshine0523.freeform.ui.freeform.FreeformWindowManager;
import io.sunshine0523.freeform.util.DataHelper;
import io.sunshine0523.freeform.util.MLog;
import io.sunshine0523.freeform.util.Settings;

public class MiFreeformUIService extends IMiFreeformUIService.Stub {

    private static final String TAG = "Mi-Freeform/MiFreeformUIService";

    private Context systemContext = null;
    private MiFreeformDisplayAdapter miFreeformDisplayAdapter = null;
    private MiFreeformService miFreeformService = null;
    private Handler uiHandler = null;
    private Handler handler = null;
    private Settings settings;
    //private SideBarService sideBarService;
    private FreeformNotificationListener notificationListener;

    public MiFreeformUIService(Context context, MiFreeformDisplayAdapter miFreeformDisplayAdapter, MiFreeformService miFreeformService, Handler uiHandler, Handler handler) {
        if (null == context || null == miFreeformDisplayAdapter || null == miFreeformService || null == uiHandler || null == handler) return;

        this.systemContext = context;
        this.miFreeformDisplayAdapter = miFreeformDisplayAdapter;
        this.miFreeformService = miFreeformService;
        this.uiHandler = uiHandler;
        this.handler = handler;
        this.settings = DataHelper.INSTANCE.getSettings();

        SystemServiceHolder.init(() -> {
            try {
                ServiceManager.addService("mi_freeform", this);
                Map<String, IBinder> cache = new ArrayMap<>();
                cache.put("mi_freeform", this);
                ServiceManager.initServiceCache(cache);
                MLog.i(TAG, "add mi_freeform SystemService: " + ServiceManager.getService("mi_freeform"));
            } catch (Exception e) {
                MLog.e(TAG, "add mi_freeform service failed, " + e);
            }
            if (ServiceManager.getService("mi_freeform") == null) return;
            //this.sideBarService = new SideBarService(context, uiHandler, settings);
            initNotificationListener();
        });
    }

    @Override
    public void startAppInFreeform(
            String packageName, String activityName, int userId, PendingIntent pendingIntent,
            int width, int height, int densityDpi, float refreshRate,
            boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations,
            String resPkg, String layoutName) {
        MLog.i(TAG, "startAppInFreeform");
        FreeformWindowManager.addWindow(
                handler, systemContext,
                packageName, activityName, userId, pendingIntent,
                width, height, densityDpi, refreshRate,
                secure, ownContentOnly, shouldShowSystemDecorations,
                resPkg, layoutName);
    }

    @Override
    public void removeFreeform(String freeformId) {
        FreeformWindowManager.removeWindow(freeformId);
    }

    @Override
    public void createFreeformInUser(
            String name, int width, int height, int densityDpi, float refreshRate,
            boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations,
            Surface surface, IMiFreeformDisplayCallback callback
    ) {
        miFreeformDisplayAdapter.createFreeformLocked(
                name, callback,
                width, height, densityDpi,
                secure, ownContentOnly, shouldShowSystemDecorations,
                surface, refreshRate, 1666666L
        );
    }

    @Override
    public void resizeFreeform(IBinder appToken, int width, int height, int densityDpi) {
        miFreeformDisplayAdapter.resizeFreeform(appToken, width, height, densityDpi);
    }

    @Override
    public void releaseFreeform(IBinder appToken) {
        miFreeformDisplayAdapter.releaseFreeform(appToken);
    }

    @Override
    public boolean ping() {
        // need inputManager is not null
        return miFreeformService.isRunning();
    }

    @Override
    public String getSettings() {
        return DataHelper.INSTANCE.getSettingsString();
    }

    @Override
    public void setSettings(String settings) {
        DataHelper.INSTANCE.saveSettings(settings, () -> {
            //sideBarService.onChanged();
            notificationListener.onChanged();
            FreeformWindowManager.settings = DataHelper.INSTANCE.getSettings();
        });
    }

    @Override
    public String getLog() {
        return DataHelper.INSTANCE.getLog();
    }

    @Override
    public void clearLog() {
        DataHelper.INSTANCE.clearLog();
    }

    @Override
    public void collapseStatusBar() {
        handler.post(() -> SystemServiceHolder.statusBarService.collapsePanels());
    }

    @Override
    public void cancelNotification(String key) {
        handler.post(() -> SystemServiceHolder.notificationManager.cancelNotificationsFromListener(notificationListener, new String[]{key}));
    }

    private void initNotificationListener() {
        try {
            Context userContext = systemContext.createPackageContext("com.sunshine.freeform", CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY);
            NotificationManager notificationManager = (NotificationManager) systemContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationListener = new FreeformNotificationListener(userContext, notificationManager, handler);
            SystemServiceHolder.notificationManager.registerListener(
                    notificationListener,
                    new ComponentName("com.sunshine.freeform", "com.sunshine.freeform.ui.main.MainActivity"),
                    0
            );
        } catch (Exception e) {
            MLog.e(TAG, "register notification listener failed: " + e);
        }
    }

}
