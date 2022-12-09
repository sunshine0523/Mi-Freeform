package com.sunshine.freeform.hook

import android.app.Activity
import android.app.AndroidAppHelper
import android.app.Application
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.view.View
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


class HookLauncher : IXposedHookLoadPackage {

    private var mUserContext: Context? = null

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hookLauncherAfterQ(param.classLoader)
        }
    }

    private fun hookLauncherAfterQ(classLoader: ClassLoader) {
        XposedBridge.hookAllMethods(XposedHelpers.findClass("com.android.quickstep.TaskOverlayFactory", classLoader), "getEnabledShortcuts", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val taskView = param.args[0] as View
                val shortcuts = param.result as MutableList<Any>
                val itemInfo = XposedHelpers.getObjectField(shortcuts[0], "mItemInfo")
                val topComponent = XposedHelpers.callMethod(itemInfo, "getTargetComponent") as ComponentName
                val activity = taskView.getActivity()

                val task = XposedHelpers.callMethod(taskView, "getTask")
                val key = XposedHelpers.getObjectField(task, "key")
                val userId = XposedHelpers.getIntField(key, "userId")

                val class_RemoteActionShortcut = XposedHelpers.findClass("com.android.launcher3.popup.RemoteActionShortcut", classLoader)
                val intent = Intent("com.sunshine.freeform.start_freeform").apply {
                    setPackage("com.sunshine.freeform")
                    putExtra("packageName", topComponent.packageName)
                    putExtra("activityName", topComponent.className)
                    putExtra("userId", userId)
                }
                val action = RemoteAction(
                    Icon.createWithResource(getUserContext(), R.drawable.tile_icon),
                    getUserContext().getString(R.string.recent_open_by_freeform),
                    "",
                    PendingIntent.getBroadcast(
                        AndroidAppHelper.currentApplication(),
                        0,
                        intent,
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                val c = class_RemoteActionShortcut.constructors[0]
                val shortcut = when (c.parameterCount) {
                    4 -> c.newInstance(action, activity, itemInfo, null)
                    3 -> c.newInstance(action, activity, itemInfo)
                    else -> {
                        XposedBridge.log("Mi-Freeform: unknown RemoteActionShortcut constructor: ${c.toGenericString()}")
                        null
                    }
                }

                if (shortcut != null) {
                    shortcuts.add(shortcut)
                }
            }
        })
    }

    private fun getUserContext(): Context {
        return if (null != mUserContext) mUserContext!!
        else {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
            val application = activityThread.getMethod("getApplication").invoke(currentActivityThread) as Application
            mUserContext = application.createPackageContext(MiFreeform.PACKAGE_NAME, Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
            mUserContext!!
        }
    }

    fun View.getActivity(): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }
}