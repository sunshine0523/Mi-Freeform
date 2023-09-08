package android.app;

import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.window.TaskSnapshot;

public interface ITaskStackListener extends IInterface {
    /** Called whenever there are changes to the state of tasks in a stack. */
    void onTaskStackChanged();

    /** Called whenever an Activity is moved to the pinned stack from another stack. */
    void onActivityPinned(String packageName, int userId, int taskId, int stackId);

    /** Called whenever an Activity is moved from the pinned stack to another stack. */
    void onActivityUnpinned();

    /**
     * Called whenever IActivityManager.startActivity is called on an activity that is already
     * running, but the task is either brought to the front or a new Intent is delivered to it.
     *
     * @param task information about the task the activity was relaunched into
     * @param homeVisible whether or not the home task is visible
     * @param clearedTask whether or not the launch activity also cleared the task as a part of
     * starting
     * @param wasVisible whether the activity was visible before the restart attempt
     */
    void onActivityRestartAttempt(ActivityManager.RunningTaskInfo task, boolean homeTaskVisible,
                                  boolean clearedTask, boolean wasVisible);

    /**
     * Called when we launched an activity that we forced to be resizable.
     *
     * @param packageName Package name of the top activity in the task.
     * @param taskId Id of the task.
     * @param reason {@link #FORCED_RESIZEABLE_REASON_SPLIT_SCREEN} or
     *              {@link #FORCED_RESIZEABLE_REASON_SECONDARY_DISPLAY}.
     */
    void onActivityForcedResizable(String packageName, int taskId, int reason);

    /**
     * Called when we launched an activity that dismissed the docked task.
     */
    void onActivityDismissingDockedTask();

    /**
     * Called when an activity was requested to be launched on a secondary display but was not
     * allowed there.
     * Only for A8.1, A9
     */
    void onActivityLaunchOnSecondaryDisplayFailed();

    /**
     * Called when an activity was requested to be launched on a secondary display but was not
     * allowed there.
     *
     * @param taskInfo info about the Activity's task
     * @param requestedDisplayId the id of the requested launch display
     */
    void onActivityLaunchOnSecondaryDisplayFailed(ActivityManager.RunningTaskInfo taskInfo,
                                                  int requestedDisplayId);

    /**
     * Called when an activity was requested to be launched on a secondary display but was rerouted
     * to default display.
     *
     * @param taskInfo info about the Activity's task
     * @param requestedDisplayId the id of the requested launch display
     */
    void onActivityLaunchOnSecondaryDisplayRerouted(ActivityManager.RunningTaskInfo taskInfo,
                                                    int requestedDisplayId);

    /**
     * Called when a task is added.
     *
     * @param taskId id of the task.
     * @param componentName of the activity that the task is being started with.
     */
    void onTaskCreated(int taskId, ComponentName componentName);

    /**
     * Called when a task is removed.
     *
     * @param taskId id of the task.
     */
    void onTaskRemoved(int taskId);

    /**
     * Called when a task is moved to the front of its stack.
     *
     * @param taskId id of the task.
     * Only for A8.1,A9
     */
    void onTaskMovedToFront(int taskId);

    /**
     * Called when a task is moved to the front of its stack.
     *
     * @param taskInfo info about the task which moved
     */
    void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo);

    /**
     * Called when a task’s description is changed due to an activity calling
     * ActivityManagerService.setTaskDescription
     *
     * @param taskId id of the task.
     * @param td the new TaskDescription.
     *
     * Only for A8.1,A9
     */
    void onTaskDescriptionChanged(int taskId, ActivityManager.TaskDescription td);

    /**
     * Called when a task’s description is changed due to an activity calling
     * ActivityManagerService.setTaskDescription
     *
     * @param taskInfo info about the task which changed, with {@link TaskInfo#taskDescription}
     */
    void onTaskDescriptionChanged(ActivityManager.RunningTaskInfo taskInfo);

    /**
     * Called when a activity’s orientation is changed due to it calling
     * ActivityManagerService.setRequestedOrientation
     *
     * @param taskId id of the task that the activity is in.
     * @param requestedOrientation the new requested orientation.
     */
    void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation);

    /**
     * Called when the task is about to be finished but before its surfaces are
     * removed from the window manager. This allows interested parties to
     * perform relevant animations before the window disappears.
     * Only For A8.1, A9
     */
    void onTaskRemovalStarted(int taskId);

    /**
     * Called when the task is about to be finished but before its surfaces are
     * removed from the window manager. This allows interested parties to
     * perform relevant animations before the window disappears.
     *
     * @param taskInfo info about the task being removed
     */
    void onTaskRemovalStarted(ActivityManager.RunningTaskInfo taskInfo);

    /**
     * Called when the task has been put in a locked state because one or more of the
     * activities inside it belong to a managed profile user, and that user has just
     * been locked.
     * Only for A8.1,A9
     */
    void onTaskProfileLocked(int taskId, int userId);

    /**
     * Called when the task has been put in a locked state because one or more of the
     * activities inside it belong to a managed profile user, and that user has just
     * been locked.
     */
    void onTaskProfileLocked(ActivityManager.RunningTaskInfo taskInfo);

    /**
     * Called when a task snapshot got updated.
     * For A11,A10
     */
    void onTaskSnapshotChanged(int taskId, ActivityManagerHidden.TaskSnapshot snapshot);

    /**
     * Called when a task snapshot got updated.
     */
    void onTaskSnapshotChanged(int taskId, TaskSnapshot snapshot);

    /**
     * Reports that an Activity received a back key press when there were no additional activities
     * on the back stack.
     *
     * @param taskInfo info about the task which received the back press
     *
     * For A13,A12
     */
    void onBackPressedOnTaskRoot(ActivityManager.RunningTaskInfo taskInfo);

    /**
     * Called when a task is reparented to a stack on a different display.
     *
     * @param taskId id of the task which was moved to a different display.
     * @param newDisplayId id of the new display.
     */
    void onTaskDisplayChanged(int taskId, int newDisplayId);

    /**
     * Called when any additions or deletions to the recent tasks list have been made.
     */
    void onRecentTaskListUpdated();

    /**
     * Called when Recent Tasks list is frozen or unfrozen.
     *
     * @param frozen if true, Recents Tasks list is currently frozen, false otherwise
     */
    void onRecentTaskListFrozenChanged(boolean frozen);

    /**
     * Called when a task gets or loses focus.
     *
     * @param taskId id of the task.
     * @param {@code true} if the task got focus, {@code false} if it lost it.
     */
    void onTaskFocusChanged(int taskId, boolean focused);

    /**
     * Called when a task changes its requested orientation. It is different from {@link
     * #onActivityRequestedOrientationChanged(int, int)} in the sense that this method is called
     * when a task changes requested orientation due to activity launch, dimiss or reparenting.
     *
     * @param taskId id of the task.
     * @param requestedOrientation the new requested orientation of this task as screen orientations
     *                             in {@link android.content.pm.ActivityInfo}.
     */
    void onTaskRequestedOrientationChanged(int taskId, int requestedOrientation);

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

    /**
     * Called when a task is moved to the back behind the home stack.
     *
     * @param taskInfo info about the task which moved
     */
    void onTaskMovedToBack(ActivityManager.RunningTaskInfo taskInfo);

    /**
     * Called when the lock task mode changes. See ActivityManager#LOCK_TASK_MODE_* and
     * LockTaskController.
     */
    void onLockTaskModeChanged(int mode);

    abstract class Stub extends Binder implements ITaskStackListener {
        @Override
        public IBinder asBinder() {
            throw new RuntimeException("Stub!");
        }
    }
}
