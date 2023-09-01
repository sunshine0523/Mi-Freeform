/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.display;

import android.content.Context;
import android.os.IBinder;
import android.view.Surface;
import android.view.SurfaceControlHidden;

/**
 * Represents a physical display device such as the built-in display
 * an external monitor, or a WiFi display.
 * <p>
 * Display devices are guarded by the {@link DisplayManagerService.SyncRoot} lock.
 * </p>
 */
abstract class DisplayDevice {

    public DisplayDevice(DisplayAdapter displayAdapter, IBinder displayToken, String uniqueId,
                         Context context) {
        throw new RuntimeException("Stub!");
    }

    public DisplayDevice(DisplayAdapter displayAdapter, IBinder displayToken, String uniqueId) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Gets the Surface Flinger display token for this display.
     *
     * @return The display token, or null if the display is not being managed
     * by Surface Flinger.
     */
    public final IBinder getDisplayTokenLocked() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the unique id of the display device.
     */
    public final String getUniqueId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns whether the unique id of the device is stable across reboots.
     */
    public abstract boolean hasStableUniqueId();

    /**
     * Gets information about the display device.
     *
     * The information returned should not change between calls unless the display
     * adapter sent a {@link DisplayAdapter#DISPLAY_DEVICE_EVENT_CHANGED} event and
     * {@link #applyPendingDisplayDeviceInfoChangesLocked()} has been called to apply
     * the pending changes.
     *
     * @return The display device info, which should be treated as immutable by the caller.
     * The display device should allocate a new display device info object whenever
     * the data changes.
     */
    public abstract DisplayDeviceInfo getDisplayDeviceInfoLocked();

    /**
     * Gives the display device a chance to update its properties while in a transaction.
     */
    public void performTraversalLocked(SurfaceControlHidden.Transaction t) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the display surface while in a transaction.
     */
    public final void setSurfaceLocked(SurfaceControlHidden.Transaction t, Surface surface) {
        throw new RuntimeException("Stub!");
    }
}