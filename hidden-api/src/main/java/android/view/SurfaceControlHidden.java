package android.view;

import android.os.IBinder;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(SurfaceControl.class)
public final class SurfaceControlHidden {

    public static IBinder createDisplay(String name, boolean secure) {
        throw new RuntimeException("Stub!");
    }
    public static void destroyDisplay(IBinder displayToken) {
        throw new RuntimeException("Stub!");
    }
    public static class Transaction {
        public static void setDisplaySize(IBinder displayToken, int width, int height) {
            throw new RuntimeException("Stub!");
        }
    }
}
