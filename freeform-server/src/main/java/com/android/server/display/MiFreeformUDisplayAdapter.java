package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.DisplayShapeHidden;
import android.view.Surface;

import io.sunshine0523.freeform.IMiFreeformDisplayCallback;

/**
 * A display adapter that provides freeform displays on behalf of applications.
 * <p>
 * Display adapters are guarded by the {@link DisplayManagerService.SyncRoot} lock.
 * </p>
 * This adapter only support Android U
 */
public final class MiFreeformUDisplayAdapter extends MiFreeformDisplayAdapter {
    private final LogicalDisplayMapper mLogicalDisplayMapper;

    public MiFreeformUDisplayAdapter(
            DisplayManagerService.SyncRoot syncRoot,
            Context context,
            Handler handler,
            DisplayDeviceRepository listener,
            LogicalDisplayMapper logicalDisplayMapper,
            Handler uiHandler
    ) {
        super(syncRoot, context, handler, listener, uiHandler, TAG);
        mLogicalDisplayMapper = logicalDisplayMapper;
    }

    @Override
    public void createFreeformLocked(String name, IMiFreeformDisplayCallback callback,
                                     int width, int height, int densityDpi,
                                     boolean secure, boolean ownContentOnly, boolean shouldShowSystemDecorations,
                                     Surface surface, float refreshRate, long presentationDeadlineNanos) {
        synchronized (getSyncRoot()) {
            IBinder appToken = callback.asBinder();
            FreeformFlags flags = new FreeformFlags(secure, ownContentOnly, shouldShowSystemDecorations);
            IBinder displayToken = DisplayControl.createDisplay(UNIQUE_ID_PREFIX + name, flags.mSecure, refreshRate);
            FreeformDisplayDevice device = new FreeformUDisplayDevice(displayToken, UNIQUE_ID_PREFIX + name, width, height, densityDpi,
                    refreshRate, presentationDeadlineNanos,
                    flags, surface, new Callback(callback, mHandler), callback.asBinder());

            sendDisplayDeviceEventLocked(device, DISPLAY_DEVICE_EVENT_ADDED);
            mFreeformDisplayDevices.put(appToken, device);
            miFreeformDisplayCallbackArrayMap.put(device, callback);

            mHandler.postDelayed(() -> {
                LogicalDisplay display = mLogicalDisplayMapper.getDisplayLocked(device);
                Log.i(TAG, "findLogicalDisplayForDevice " + display);
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

    @Override
    public void resizeFreeform(IBinder appToken, int width, int height, int densityDpi) {
        super.resizeFreeform(appToken, width, height, densityDpi);
    }

    @Override
    public void releaseFreeform(IBinder appToken) {
        super.releaseFreeform(appToken);
    }

    private class FreeformUDisplayDevice extends FreeformDisplayDevice {

        FreeformUDisplayDevice(IBinder displayToken, String uniqueId,
                               int width, int height, int density,
                               float refreshRate, long presentationDeadlineNanos,
                               FreeformFlags flags, Surface surface,
                               Callback callback, IBinder appToken) {
            super(displayToken, uniqueId,
                    width, height, density,
                    refreshRate, presentationDeadlineNanos,
                    flags, surface, callback, appToken);
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            super.getDisplayDeviceInfoLocked();
            mInfo.displayShape = DisplayShapeHidden.createDefaultDisplayShape(mInfo.width, mInfo.height, false);

            return mInfo;
        }

        @Override
        public void destroyLocked(boolean binderAlive) {
            if (mSurface != null) {
                mSurface.release();
                mSurface = null;
            }
            DisplayControl.destroyDisplay(getDisplayTokenLocked());
            if (binderAlive) {
                mCallback.dispatchDisplayStopped();
            }

        }
    }
}
