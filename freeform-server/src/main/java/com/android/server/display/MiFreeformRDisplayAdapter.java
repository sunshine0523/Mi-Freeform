package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceControlHidden;

import io.sunshine0523.freeform.IMiFreeformDisplayCallback;
import io.sunshine0523.freeform.util.MLog;

/**
 * A display adapter that provides freeform displays on behalf of applications.
 * <p>
 * Display adapters are guarded by the {@link DisplayManagerService.SyncRoot} lock.
 * </p>
 * This adapter only support Android Q,R
 */
public final class MiFreeformRDisplayAdapter extends MiFreeformDisplayAdapter {
    private final SparseArray<LogicalDisplay> mLogicalDisplays;

    public MiFreeformRDisplayAdapter(
            DisplayManagerService.SyncRoot syncRoot,
            Context context,
            Handler handler,
            Listener listener,
            SparseArray<LogicalDisplay> mLogicalDisplays,
            Handler uiHandler
    ) {
        super(syncRoot, context, handler, listener, uiHandler, TAG);
        this.mLogicalDisplays = mLogicalDisplays;
    }

    @Override
    public void createFreeformLocked(String name, IMiFreeformDisplayCallback callback,
                                    int width, int height, int densityDpi,
                                    boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations,
                                    Surface surface, float refreshRate, long presentationDeadlineNanos) {
        synchronized (getSyncRoot()) {
            IBinder appToken = callback.asBinder();
            FreeformFlags flags = new FreeformFlags(secure, ownContentOnly, shouldShowSystemDecorations);
            IBinder displayToken = SurfaceControlHidden.createDisplay(UNIQUE_ID_PREFIX + name, flags.mSecure);
            FreeformDisplayDevice device = new FreeformDisplayDevice(displayToken, UNIQUE_ID_PREFIX + name, width, height, densityDpi,
                    refreshRate, presentationDeadlineNanos,
                    flags, surface, new Callback(callback, mHandler), callback.asBinder(), true);

            sendDisplayDeviceEventLocked(device, DISPLAY_DEVICE_EVENT_ADDED);
            mFreeformDisplayDevices.put(appToken, device);
            miFreeformDisplayCallbackArrayMap.put(device, callback);

            mHandler.postDelayed(() -> {
                LogicalDisplay display = findLogicalDisplayForDevice(device);
                MLog.i(TAG, "findLogicalDisplayForDevice " + display);
                try {
                    callback.onDisplayAdd(display.getDisplayIdLocked());
                } catch (Exception ignored) {

                }
            }, 500);

            try {
                appToken.linkToDeath(device, 0);
            } catch (RemoteException ex) {
                mFreeformDisplayDevices.remove(appToken);
                device.destroyLocked(false);
            }
        }
    }

    private LogicalDisplay findLogicalDisplayForDevice(DisplayDevice device) {
        synchronized (getSyncRoot()) {
            final int count = mLogicalDisplays.size();
            for (int i = 0; i < count; i++) {
                LogicalDisplay display = mLogicalDisplays.valueAt(i);
                if (display.getPrimaryDisplayDeviceLocked() == device) {
                    return display;
                }
            }
            return null;
        }
    }

    @Override
    public void resizeFreeform(IBinder appToken, int width, int height, int densityDpi) {
        super.resizeFreeform(appToken, width, height, densityDpi);
    }

    @Override
    public void releaseFreeform(IBinder appToken) {
        super.releaseFreeform(appToken);
    }

}
