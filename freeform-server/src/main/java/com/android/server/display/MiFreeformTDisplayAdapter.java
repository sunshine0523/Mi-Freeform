package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceControlHidden;

import io.sunshine0523.freeform.IMiFreeformDisplayCallback;
import io.sunshine0523.freeform.util.MLog;

/**
 * A display adapter that provides freeform displays on behalf of applications.
 * <p>
 * Display adapters are guarded by the {@link DisplayManagerService.SyncRoot} lock.
 * </p>
 * This adapter only support Android S,T
 */
public final class MiFreeformTDisplayAdapter extends MiFreeformDisplayAdapter {
    private final LogicalDisplayMapper mLogicalDisplayMapper;

    public MiFreeformTDisplayAdapter(
            DisplayManagerService.SyncRoot syncRoot,
            Context context,
            Handler handler,
            DisplayDeviceRepository listener,
            LogicalDisplayMapper logicalDisplayMapper,
            Handler uiHandler
    ) {
        super(syncRoot, context, handler, listener, uiHandler, TAG);
        mLogicalDisplayMapper = logicalDisplayMapper;
        //addListener((DisplayDeviceRepository) listener);
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
                    flags, surface, new Callback(callback, mHandler), callback.asBinder());

            sendDisplayDeviceEventLocked(device, DISPLAY_DEVICE_EVENT_ADDED);
            mFreeformDisplayDevices.put(appToken, device);
            miFreeformDisplayCallbackArrayMap.put(device, callback);

            mHandler.postDelayed(() -> {
                LogicalDisplay display = mLogicalDisplayMapper.getDisplayLocked(device);
                MLog.i(TAG, "findLogicalDisplayForDevice " + display);
                try {
                    callback.onDisplayAdd(display.getDisplayIdLocked());
                } catch (Exception e) {

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

    @Override
    public void resizeFreeform(IBinder appToken, int width, int height, int densityDpi) {
        super.resizeFreeform(appToken, width, height, densityDpi);
    }

    @Override
    public void releaseFreeform(IBinder appToken) {
        super.releaseFreeform(appToken);
    }

    /**
     * Add Listener. When added LogicalAdapter, callback to user
     */
    private void addListener(DisplayDeviceRepository listener) {
        listener.addListener(new DisplayDeviceRepository.Listener() {
            @Override
            public void onDisplayDeviceEventLocked(DisplayDevice device, int event) {
                synchronized (getSyncRoot()) {
                    if (device instanceof FreeformDisplayDevice) {
                        if (event == DISPLAY_DEVICE_EVENT_ADDED) {
                            final LogicalDisplay display = mLogicalDisplayMapper.getDisplayLocked(device);
                            if (display != null) {
                                IMiFreeformDisplayCallback callback =
                                        miFreeformDisplayCallbackArrayMap.get(device);
                                try {
                                    if (null != callback) {
                                        MLog.i(TAG, "create freeform display: " + display);
                                        callback.onDisplayAdd(display.getDisplayIdLocked());
                                    }
                                } catch (RemoteException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onTraversalRequested() {

            }
        });
    }
}
