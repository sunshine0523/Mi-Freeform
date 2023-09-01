package io.sunshine0523.freeform.service;

import android.view.InputEvent;
import android.view.Surface;

import com.android.server.display.MiFreeformDisplayAdapter;

import io.sunshine0523.freeform.IMiFreeformDisplayCallback;
import io.sunshine0523.freeform.util.MLog;

public class MiFreeformService {
    private static final String TAG = "Mi-Freeform/MiFreeformService";

    private MiFreeformDisplayAdapter miFreeformDisplayAdapter = null;

    public MiFreeformService(MiFreeformDisplayAdapter miFreeformDisplayAdapter) {
        this.miFreeformDisplayAdapter = miFreeformDisplayAdapter;
    }

    public void createFreeform(String name, IMiFreeformDisplayCallback callback,
                               int width, int height, int densityDpi, boolean secure,
                               boolean ownContentOnly, boolean shouldShowSystemDecorations, Surface surface,
                               float refreshRate, long presentationDeadlineNanos) {
        miFreeformDisplayAdapter.createFreeformLocked(name, callback,
                width, height, densityDpi, secure,
                ownContentOnly, shouldShowSystemDecorations, surface,
                refreshRate, presentationDeadlineNanos);
        MLog.i(TAG, "createFreeform");
    }

    public void injectInputEvent(InputEvent event, int displayId) {
        try {
            event.getClass().getMethod("setDisplayId", int.class).invoke(event, displayId);
            SystemServiceHolder.inputManagerService.injectInputEvent(event, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRunning() {
        return null != SystemServiceHolder.inputManagerService;
    }
}
