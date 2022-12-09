/*
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package android.view;

import android.view.IRotationWatcher;

interface IWindowManager {
    /**
     * Lock the display orientation to the specified rotation, or to the current
     * rotation if -1. Sensor input will be ignored until thawRotation() is called.
     *
     * @param displayId the ID of display which rotation should be frozen.
     * @param rotation one of {@link android.view.Surface#ROTATION_0},
     *        {@link android.view.Surface#ROTATION_90}, {@link android.view.Surface#ROTATION_180},
     *        {@link android.view.Surface#ROTATION_270} or -1 to freeze it to current rotation.
     * @hide
     */
    void freezeDisplayRotation(int displayId, int rotation);

    /**
     * Release the orientation lock imposed by freezeRotation() on the display.
     *
     * @param displayId the ID of display which rotation should be thawed.
     * @hide
     */
    void thawDisplayRotation(int displayId);

    /**
     * Gets whether the rotation is frozen on the display.
     *
     * @param displayId the ID of display which frozen is needed.
     * @return Whether the rotation is frozen.
     */
    boolean isDisplayRotationFrozen(int displayId);

    /**
     * Watch the rotation of the specified screen.  Returns the current rotation,
     * calls back when it changes.
     */
    int watchRotation(IRotationWatcher watcher, int displayId);

    /**
     * Remove a rotation watcher set using watchRotation.
     */
    void removeRotationWatcher(IRotationWatcher watcher);
}