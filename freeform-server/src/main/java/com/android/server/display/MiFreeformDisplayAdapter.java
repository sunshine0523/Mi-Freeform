package com.android.server.display;

import static com.android.server.display.DisplayDeviceInfo.FLAG_TRUSTED;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
//@RefineAs(Display.class)
import android.view.DisplayHidden;
import android.view.Surface;
//@RefineAs(SurfaceControl.class)
import android.view.SurfaceControlHidden;

import java.io.PrintWriter;

import io.sunshine0523.freeform.IMiFreeformDisplayCallback;

/**
 * A display adapter that provides freeform displays on behalf of applications.
 * <p>
 * Display adapters are guarded by the {@link DisplayManagerService.SyncRoot} lock.
 * </p>
 */
public final class MiFreeformDisplayAdapter extends DisplayAdapter {

    static final String TAG = "Mi-Freeform/MiFreeformDisplayAdapter";

    // Unique id prefix for freeform displays.
    private static final String UNIQUE_ID_PREFIX = "mi-freeform:";

    private final ArrayMap<IBinder, FreeformDisplayDevice> mFreeformDisplayDevices =
            new ArrayMap<>();
    private final ArrayMap<FreeformDisplayDevice, IMiFreeformDisplayCallback> miFreeformDisplayCallbackArrayMap =
            new ArrayMap<>();

    private final Handler mHandler;
    private final LogicalDisplayMapper mLogicalDisplayMapper;
    private final Handler mUiHandler;

    public MiFreeformDisplayAdapter(
            DisplayManagerService.SyncRoot syncRoot,
            Context context,
            Handler handler,
            DisplayDeviceRepository listener,
            LogicalDisplayMapper logicalDisplayMapper,
            Handler uiHandler
    ) {
        super(syncRoot, context, handler, listener, TAG);
        mHandler = handler;
        mLogicalDisplayMapper = logicalDisplayMapper;
        mUiHandler = uiHandler;

        addListener(listener);
    }

    @Override
    public void dumpLocked(PrintWriter pw) {
        super.dumpLocked(pw);
    }

    @Override
    public void registerLocked() {
        super.registerLocked();
    }

    /**
     * @return displayId
     */
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

            try {
                appToken.linkToDeath(device, 0);
            } catch (RemoteException ex) {
                mFreeformDisplayDevices.remove(appToken);
                device.destroyLocked(false);
            }

            Log.i(TAG, "createFreeformLocked");
        }
    }

    public void resizeFreeform(IBinder appToken,
                               int width, int height, int densityDpi) {
        FreeformDisplayDevice device = mFreeformDisplayDevices.get(appToken);
        if (device != null) {
            device.resizeLocked(width, height, densityDpi);
            Log.i(TAG, "resize freeform display: " + appToken);
        }
    }

    public void releaseFreeform(IBinder appToken) {
        FreeformDisplayDevice device = mFreeformDisplayDevices.remove(appToken);
        if (device != null) {
            miFreeformDisplayCallbackArrayMap.remove(device);

            device.destroyLocked(true);
            appToken.unlinkToDeath(device, 0);
            Log.i(TAG, "release freeform display: " + appToken);
        }
    }

    private void handleBinderDiedLocked(IBinder appToken) {
        FreeformDisplayDevice device = mFreeformDisplayDevices.remove(appToken);
        if (device != null) {
            miFreeformDisplayCallbackArrayMap.remove(device);
        }
    }

    /**
     * 添加监听，在添加好屏幕后，给用户回调
     */
    public void addListener(DisplayDeviceRepository listener) {
        listener.addListener(new DisplayDeviceRepository.Listener() {
            @Override
            public void onDisplayDeviceEventLocked(DisplayDevice device, int event) {
                synchronized (getSyncRoot()) {
                    if (device instanceof FreeformDisplayDevice) {
                        Log.i(TAG, "onDisplayDeviceEvent received: " + device + " " + event);
                        if (event == DISPLAY_DEVICE_EVENT_ADDED) {
                            final LogicalDisplay display = mLogicalDisplayMapper.getDisplayLocked(device);
                            if (display != null) {
                                IMiFreeformDisplayCallback callback =
                                        miFreeformDisplayCallbackArrayMap.get(device);
                                try {
                                    if (null != callback) {
                                        Log.i(TAG, "create freeform display: " + display);
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

    public Handler getUiHandler() {
        return mUiHandler;
    }

    public class FreeformDisplayDevice extends DisplayDevice implements IBinder.DeathRecipient {
        private static final int PENDING_SURFACE_CHANGE = 0x01;
        private static final int PENDING_RESIZE = 0x02;

        private String mName;
        private final float mRefreshRate;
        private final long mDisplayPresentationDeadlineNanos;
        private final FreeformFlags mFlags;
        private int mWidth;
        private int mHeight;
        private int mDensityDpi;
        private DisplayHidden.Mode mMode;
        private Surface mSurface;
        private DisplayDeviceInfo mInfo;

        private final Callback mCallback;
        private final IBinder mAppToken;

        private int mPendingChanges;

        FreeformDisplayDevice(IBinder displayToken, String uniqueId,
                              int width, int height, int density,
                              float refreshRate, long presentationDeadlineNanos,
                              FreeformFlags flags,
                              Surface surface, Callback callback, IBinder appToken) {
            super(MiFreeformDisplayAdapter.this, displayToken, uniqueId,
                    getContext());
            mName = uniqueId;
            mRefreshRate = refreshRate;
            mDisplayPresentationDeadlineNanos = presentationDeadlineNanos;
            mFlags = flags;
            mSurface = surface;
            mWidth = width;
            mHeight = height;
            mDensityDpi = density;
            mMode = createMode(mWidth, mHeight, refreshRate);
            mCallback = callback;
            mAppToken = appToken;
            mPendingChanges |= PENDING_SURFACE_CHANGE;
        }

        public void resizeLocked(int width, int height, int densityDpi) {
            if (mWidth != width || mHeight != height || mDensityDpi != densityDpi) {
                sendDisplayDeviceEventLocked(this, DISPLAY_DEVICE_EVENT_CHANGED);
                sendTraversalRequestLocked();
                mWidth = width;
                mHeight = height;
                mMode = createMode(width, height, mRefreshRate);
                mDensityDpi = densityDpi;
                mInfo = null;
            }
        }

        public void destroyLocked(boolean binderAlive) {
            if (mSurface != null) {
                mSurface.release();
                mSurface = null;
            }
            SurfaceControlHidden.destroyDisplay(getDisplayTokenLocked());
            if (binderAlive) {
                mCallback.dispatchDisplayStopped();
            }
        }

        @Override
        public boolean hasStableUniqueId() {
            return false;
        }

        @Override
        public void performTraversalLocked(SurfaceControlHidden.Transaction t) {
            if ((mPendingChanges & PENDING_RESIZE) != 0) {
                t.setDisplaySize(getDisplayTokenLocked(), mWidth, mHeight);
            }
            if ((mPendingChanges & PENDING_SURFACE_CHANGE) != 0) {
                setSurfaceLocked(t, mSurface);
            }
            mPendingChanges = 0;
        }

        @Override
        public void binderDied() {
            synchronized (getSyncRoot()) {
                handleBinderDiedLocked(mAppToken);
                Log.i(TAG, "Freeform display device released because application token died: "
                        + mAppToken);
                destroyLocked(false);
                sendDisplayDeviceEventLocked(this, DISPLAY_DEVICE_EVENT_REMOVED);
            }
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (mInfo == null) {
                mInfo = new DisplayDeviceInfo();
                mInfo.name = mName;
                mInfo.uniqueId = getUniqueId();
                mInfo.width = mMode.getPhysicalWidth();
                mInfo.height = mMode.getPhysicalHeight();
                mInfo.modeId = mMode.getModeId();
                mInfo.defaultModeId = mMode.getModeId();
                mInfo.supportedModes = new DisplayHidden.Mode[] { mMode };
                mInfo.densityDpi = mDensityDpi;
                mInfo.xDpi = mDensityDpi;
                mInfo.yDpi = mDensityDpi;
                mInfo.presentationDeadlineNanos = mDisplayPresentationDeadlineNanos +
                        1000000000L / (int) mRefreshRate;   // display's deadline + 1 frame
                mInfo.flags = DisplayDeviceInfo.FLAG_PRESENTATION;
                if (mFlags.mSecure) {
                    mInfo.flags |= DisplayDeviceInfo.FLAG_SECURE;
                }
                if (mFlags.mOwnContentOnly) {
                    mInfo.flags |= DisplayDeviceInfo.FLAG_OWN_CONTENT_ONLY;
                }
                if (mFlags.mShouldShowSystemDecorations) {
                    mInfo.flags |= DisplayDeviceInfo.FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS;
                }
                mInfo.type = DisplayHidden.TYPE_OVERLAY;
                mInfo.touch = DisplayDeviceInfo.TOUCH_VIRTUAL;
                // The display is trusted since it is created by system.
                mInfo.flags |= FLAG_TRUSTED;
            }
            return mInfo;
        }
    }

    /** Represents the flags of the freeform display. */
    private static final class FreeformFlags {
        final boolean mSecure;

        final boolean mOwnContentOnly;

        final boolean mShouldShowSystemDecorations;

        FreeformFlags(
                boolean secure,
                boolean ownContentOnly,
                boolean shouldShowSystemDecorations) {
            mSecure = secure;
            mOwnContentOnly = ownContentOnly;
            mShouldShowSystemDecorations = shouldShowSystemDecorations;
        }

        @Override
        public String toString() {
            return new StringBuilder("{")
                    .append("secure=").append(mSecure)
                    .append(", ownContentOnly=").append(mOwnContentOnly)
                    .append(", shouldShowSystemDecorations=").append(mShouldShowSystemDecorations)
                    .append("}")
                    .toString();
        }
    }

    private static class Callback extends Handler{
        private static final int MSG_ON_DISPLAY_PAUSED = 0;
        private static final int MSG_ON_DISPLAY_RESUMED = 1;
        private static final int MSG_ON_DISPLAY_STOPPED = 2;

        private final IMiFreeformDisplayCallback mCallback;

        public Callback(IMiFreeformDisplayCallback callback, Handler handler) {
            super(handler.getLooper());
            mCallback = callback;
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_ON_DISPLAY_PAUSED:
                        mCallback.onDisplayPaused();
                        break;
                    case MSG_ON_DISPLAY_RESUMED:
                        mCallback.onDisplayResumed();
                        break;
                    case MSG_ON_DISPLAY_STOPPED:
                        mCallback.onDisplayStopped();
                        break;
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to notify listener of freeform display event.", e);
            }
        }

        public void dispatchDisplayPaused() {
            sendEmptyMessage(MSG_ON_DISPLAY_PAUSED);
        }

        public void dispatchDisplayResumed() {
            sendEmptyMessage(MSG_ON_DISPLAY_RESUMED);
        }

        public void dispatchDisplayStopped() {
            sendEmptyMessage(MSG_ON_DISPLAY_STOPPED);
        }
    }
}
