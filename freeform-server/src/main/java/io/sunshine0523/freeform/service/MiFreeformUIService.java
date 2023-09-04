package io.sunshine0523.freeform.service;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.view.Surface;

import com.android.server.display.MiFreeformDisplayAdapter;

import java.util.Map;

import io.sunshine0523.freeform.IMiFreeformDisplayCallback;
import io.sunshine0523.freeform.IMiFreeformUIService;
import io.sunshine0523.freeform.util.DataHelper;
import io.sunshine0523.freeform.util.MLog;
import io.sunshine0523.freeform.util.Settings;

public class MiFreeformUIService extends IMiFreeformUIService.Stub {

    private static final String TAG = "Mi-Freeform/MiFreeformUIService";

    private Context systemContext = null;
    private MiFreeformDisplayAdapter miFreeformDisplayAdapter = null;
    private MiFreeformService miFreeformService = null;
    private Handler uiHandler = null;
    private Settings settings;
    private SideBarService sideBarService;

    public MiFreeformUIService(Context context, MiFreeformDisplayAdapter miFreeformDisplayAdapter, MiFreeformService miFreeformService, Handler uiHandler) {
        if (null == context || null == miFreeformDisplayAdapter || null == miFreeformService || null == uiHandler) return;

        this.systemContext = context;
        this.miFreeformDisplayAdapter = miFreeformDisplayAdapter;
        this.miFreeformService = miFreeformService;
        this.uiHandler = uiHandler;
        this.settings = DataHelper.INSTANCE.getSettings();
        SystemServiceHolder.init(() -> {
            this.sideBarService = new SideBarService(context, uiHandler, settings);
            ServiceManager.addService("mi_freeform", this);
            Map<String, IBinder> cache = new ArrayMap<>();
            cache.put("mi_freeform", this);
            ServiceManager.initServiceCache(cache);
            MLog.i(TAG, "add mi_freeform SystemService: " + this);
        });
    }

    @Override
    public void startAppInFreeform(
            ComponentName componentName, int userId,
            int width, int height, int densityDpi, float refreshRate,
            boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations,
            String resPkg, String layoutName) {
        MLog.i(TAG, "startAppInFreeform");
        FreeformWindowManager.addWindow(
                uiHandler, systemContext,
                componentName, userId,
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
        DataHelper.INSTANCE.saveSettings(settings, this.sideBarService);
    }

    @Override
    public String getLog() {
        return DataHelper.INSTANCE.getLog();
    }

    @Override
    public void clearLog() {
        DataHelper.INSTANCE.clearLog();
    }

}
