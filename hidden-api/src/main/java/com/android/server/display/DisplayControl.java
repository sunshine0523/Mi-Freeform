package com.android.server.display;

import android.os.IBinder;

public class DisplayControl {

    /**
     * Create a display in SurfaceFlinger.
     *
     * @param name The name of the display
     * @param secure Whether this display is secure.
     * @return The token reference for the display in SurfaceFlinger.
     */
    public static IBinder createDisplay(String name, boolean secure) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Create a display in SurfaceFlinger.
     *
     * @param name The name of the display
     * @param secure Whether this display is secure.
     * @param requestedRefreshRate The requested refresh rate in frames per second.
     * For best results, specify a divisor of the physical refresh rate, e.g., 30 or 60 on
     * 120hz display. If an arbitrary refresh rate is specified, the rate will be rounded
     * up or down to a divisor of the physical display. If 0 is specified, the virtual
     * display is refreshed at the physical display refresh rate.
     * @return The token reference for the display in SurfaceFlinger.
     */
    public static IBinder createDisplay(String name, boolean secure,
                                        float requestedRefreshRate) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Destroy a display in SurfaceFlinger.
     *
     * @param displayToken The display token for the display to be destroyed.
     */
    public static void destroyDisplay(IBinder displayToken) {
        throw new RuntimeException("Stub!");
    }

}
