/**
 * Copyright (c) 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app;

import android.app.ActivityManager;

interface ITaskStackListener {
    /**
     * Called when a task is added.
     *
     * @param taskId id of the task.
     * @param componentName of the activity that the task is being started with.
    */
    void onTaskCreated(int taskId, in ComponentName componentName);

    /**
     * Called when the task is about to be finished but before its surfaces are
     * removed from the window manager. This allows interested parties to
     * perform relevant animations before the window disappears.
     *
     * @param taskInfo info about the task being removed
     */
    void onTaskRemovalStarted(in ActivityManager.RunningTaskInfo taskInfo);

    /**
     * Called when a task is reparented to a stack on a different display.
     *
     * @param taskId id of the task which was moved to a different display.
     * @param newDisplayId id of the new display.
     */
    void onTaskDisplayChanged(int taskId, int newDisplayId);

    /**
     * Called when a task is moved to the front of its stack.
     *
     * @param taskInfo info about the task which moved
    */
    void onTaskMovedToFront(in ActivityManager.RunningTaskInfo taskInfo);

    /**
     * Called when a task changes its requested orientation. It is different from {@link
     * #onActivityRequestedOrientationChanged(int, int)} in the sense that this method is called
     * when a task changes requested orientation due to activity launch, dimiss or reparenting.
     *
     * @param taskId id of the task.
     * @param requestedOrientation the new requested orientation of this task as screen orientations
     *                             in {@link android.content.pm.ActivityInfo}.
     * only support 12+
     */
     void onTaskRequestedOrientationChanged(int taskId, int requestedOrientation);

    /**
     * Called when a activity’s orientation is changed due to it calling
     * ActivityManagerService.setRequestedOrientation
     *
     * @param taskId id of the task that the activity is in.
     * @param requestedOrientation the new requested orientation.
     * only support 11-
    */
    void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation);

    /**
     * Called when a rotation is about to start on the foreground activity.
     * This applies for:
     *   * free sensor rotation
     *   * forced rotation
     *   * rotation settings set through adb command line
     *   * rotation that occurs when rotation tile is toggled in quick settings
     *
     * @param displayId id of the display where activity will rotate
     */
     void onActivityRotation(int displayId);
}