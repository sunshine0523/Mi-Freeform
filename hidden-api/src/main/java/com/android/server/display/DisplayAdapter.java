package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.view.DisplayHidden;

import java.io.PrintWriter;

abstract class DisplayAdapter {

    public static final int DISPLAY_DEVICE_EVENT_ADDED = 1;
    public static final int DISPLAY_DEVICE_EVENT_CHANGED = 2;
    public static final int DISPLAY_DEVICE_EVENT_REMOVED = 3;

    public DisplayAdapter(DisplayManagerService.SyncRoot syncRoot,
                          Context context, Handler handler, Listener listener, String name) {
        throw new RuntimeException("Stub!");
    }

    public static DisplayHidden.Mode createMode(int width, int height, float refreshRate) {
        throw new RuntimeException("Stub!");
    }

    public static DisplayHidden.Mode createMode(int width, int height, float refreshRate,
                                                float[] alternativeRefreshRates) {
        throw new RuntimeException("Stub!");
    }

    public void dumpLocked(PrintWriter pw) {
        throw new RuntimeException("Stub!");
    }

    public final Context getContext() {
        throw new RuntimeException("Stub!");
    }

    public final Handler getHandler() {
        throw new RuntimeException("Stub!");
    }

    public final String getName() {
        throw new RuntimeException("Stub!");
    }

    public final DisplayManagerService.SyncRoot getSyncRoot() {
        throw new RuntimeException("Stub!");
    }

    public void registerLocked() {
        throw new RuntimeException("Stub!");
    }

    public final void sendDisplayDeviceEventLocked(final DisplayDevice device, final int event) {
        throw new RuntimeException("Stub!");
    }

    public final void sendTraversalRequestLocked() {
        throw new RuntimeException("Stub!");
    }

    public interface Listener {
        void onDisplayDeviceEvent(DisplayDevice device, int event);

        void onTraversalRequested();
    }
}

