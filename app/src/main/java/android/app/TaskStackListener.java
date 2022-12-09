/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.app;

import android.content.ComponentName;
import android.os.RemoteException;

public class TaskStackListener extends ITaskStackListener.Stub{
    @Override
    public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {

    }

    @Override
    public void onTaskRemovalStarted(ActivityManager.RunningTaskInfo taskInfo) throws RemoteException {

    }

    @Override
    public void onTaskDisplayChanged(int taskId, int newDisplayId) throws RemoteException {

    }

    @Override
    public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) throws RemoteException {

    }

    @Override
    public void onTaskRequestedOrientationChanged(int taskId, int requestedOrientation) throws RemoteException {

    }

    @Override
    public void onActivityRotation(int displayId) throws RemoteException {

    }

    @Override
    public void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation) throws RemoteException {

    }
}
