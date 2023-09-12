package io.sunshine0523.freeform.ui.freeform

import android.app.ActivityManager
import android.app.ActivityManagerHidden
import android.app.ITaskStackListener
import android.content.ComponentName
import android.os.Build
import android.util.Log
import android.window.TaskSnapshot
import androidx.annotation.RequiresApi
import io.sunshine0523.freeform.util.MLog
import kotlin.math.max
import kotlin.math.min

class FreeformTaskStackListener(
    private val displayId: Int,
    private val window: FreeformWindow
) : ITaskStackListener.Stub() {

    var taskId = -1
    //For A10
    var stackId = -1

    companion object {
        private const val TAG = "Mi-Freeform/FreeformTaskStackListener"

        const val PORTRAIT = 1
        const val LANDSCAPE_1 = 0
        const val LANDSCAPE_2 = 6
    }

    override fun onTaskStackChanged() {

    }

    override fun onActivityPinned(packageName: String, userId: Int, taskId: Int, stackId: Int) {

    }

    override fun onActivityUnpinned() {

    }

    override fun onActivityRestartAttempt(
        task: ActivityManager.RunningTaskInfo?,
        homeTaskVisible: Boolean,
        clearedTask: Boolean,
        wasVisible: Boolean
    ) {

    }

    override fun onActivityForcedResizable(packageName: String, taskId: Int, reason: Int) {

    }

    override fun onActivityDismissingDockedTask() {

    }

    override fun onActivityLaunchOnSecondaryDisplayFailed() {

    }

    override fun onActivityLaunchOnSecondaryDisplayFailed(
        taskInfo: ActivityManager.RunningTaskInfo?,
        requestedDisplayId: Int
    ) {

    }

    override fun onActivityLaunchOnSecondaryDisplayRerouted(
        taskInfo: ActivityManager.RunningTaskInfo?,
        requestedDisplayId: Int
    ) {

    }

    override fun onTaskCreated(taskId: Int, componentName: ComponentName?) {

    }

    override fun onTaskRemoved(taskId: Int) {
        Log.i(TAG, "onTaskRemoved $taskId")
    }

    override fun onTaskMovedToFront(taskId: Int) {
        Log.i(TAG, "onTaskMovedToFront $taskId")
    }

    override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (taskInfo != null) {
                val displayId = taskInfo::class.java.getField("displayId").get(taskInfo) as Int
                if (this.displayId == displayId) {
                    taskId = taskInfo.taskId
                    MLog.i(TAG, "onTaskDescriptionChanged $taskInfo")
                }
            }
        }
    }

    override fun onTaskDescriptionChanged(taskId: Int, td: ActivityManager.TaskDescription?) {
        Log.i(TAG, "onTaskDescriptionChanged $taskId $td")
    }

    override fun onTaskDescriptionChanged(taskInfo: ActivityManager.RunningTaskInfo?) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (taskInfo != null) {
                    val displayId = taskInfo::class.java.getField("displayId").get(taskInfo) as Int
                    if (this.displayId == displayId) {
                        taskId = taskInfo.taskId
                        MLog.i(TAG, "onTaskDescriptionChanged $taskInfo")
                    }
                }
            }
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                if (taskInfo != null) {
                    val displayId = taskInfo::class.java.getField("displayId").get(taskInfo) as Int
                    val stackId = taskInfo::class.java.getField("stackId").get(taskInfo) as Int
                    if (this.taskId == taskInfo.taskId && displayId != this.displayId) {
                        window.destroy(false)
                    }
                    if (this.displayId == displayId) {
                        this.taskId = taskInfo.taskId
                        this.stackId = stackId
                        MLog.i(TAG, "onTaskDescriptionChanged $taskInfo")
                    }
                }
            }
        }
    }

    override fun onActivityRequestedOrientationChanged(taskId: Int, requestedOrientation: Int) {
        Log.i(TAG, "onActivityRequestedOrientationChanged $taskId $requestedOrientation")
    }

    override fun onTaskRemovalStarted(taskId: Int) {
        Log.i(TAG, "onTaskRemovalStarted")
        if (this.taskId == taskId) {
            window.destroy(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onTaskRemovalStarted(taskInfo: ActivityManager.RunningTaskInfo?) {
        Log.i(TAG, "onTaskRemovalStarted")
        if (this.taskId == taskInfo?.taskId) {
            window.destroy(false)
        }
    }

    override fun onTaskProfileLocked(taskId: Int, userId: Int) {

    }

    override fun onTaskProfileLocked(taskInfo: ActivityManager.RunningTaskInfo?) {

    }

    override fun onTaskSnapshotChanged(taskId: Int, snapshot: ActivityManagerHidden.TaskSnapshot?) {

    }

    override fun onTaskSnapshotChanged(taskId: Int, snapshot: TaskSnapshot?) {

    }

    override fun onBackPressedOnTaskRoot(taskInfo: ActivityManager.RunningTaskInfo?) {

    }

    override fun onTaskDisplayChanged(taskId: Int, newDisplayId: Int) {

    }

    override fun onRecentTaskListUpdated() {

    }

    override fun onRecentTaskListFrozenChanged(frozen: Boolean) {

    }

    override fun onTaskFocusChanged(taskId: Int, focused: Boolean) {
        Log.i(TAG, "onTaskFocusChanged $taskId $focused")
//        if (taskId == this.taskId && !focused && !window.freeformConfig.isHangUp) {
//            window.uiHandler.post { window.handleHangUp() }
//        }
    }

    override fun onTaskRequestedOrientationChanged(taskId: Int, requestedOrientation: Int) {
        Log.i(TAG, "onTaskRequestedOrientationChanged $taskId $requestedOrientation")
        if (taskId == this.taskId) {
            val max = max(window.freeformConfig.width, window.freeformConfig.height)
            val min = min(window.freeformConfig.width, window.freeformConfig.height)
            val maxHangUp = max(window.freeformConfig.hangUpWidth, window.freeformConfig.hangUpHeight)
            val minHangUp = min(window.freeformConfig.hangUpWidth, window.freeformConfig.hangUpHeight)
            when (requestedOrientation) {
                PORTRAIT -> {
                    MLog.i(TAG, "PORTRAIT")
                    window.freeformConfig.width = min
                    window.freeformConfig.height = max
                    window.freeformConfig.hangUpWidth = minHangUp
                    window.freeformConfig.hangUpHeight = maxHangUp
                }
                LANDSCAPE_1, LANDSCAPE_2 -> {
                    MLog.i(TAG, "LANDSCAPE")
                    window.freeformConfig.width = max
                    window.freeformConfig.height = min
                    window.freeformConfig.hangUpWidth = maxHangUp
                    window.freeformConfig.hangUpHeight = minHangUp
                }
            }
            window.handler.post { window.changeOrientation() }
        }
    }

    override fun onActivityRotation(displayId: Int) {
        Log.i(TAG, "onActivityRotation display: $displayId")
    }

    override fun onTaskMovedToBack(taskInfo: ActivityManager.RunningTaskInfo?) {
        Log.i(TAG, "onTaskMovedToBack $taskInfo")
    }

    override fun onLockTaskModeChanged(mode: Int) {

    }
}