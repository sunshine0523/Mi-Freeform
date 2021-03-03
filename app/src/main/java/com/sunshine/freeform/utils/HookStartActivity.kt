package com.sunshine.freeform.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author sunshine
 * @date 2021/3/3
 */
class HookStartActivity : IXposedHookZygoteInit, IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (android.os.Build.VERSION.SDK_INT == 29) {
            hookActivityStartActivity()
        }
    }

    /**
     * 在启动时拦截并且显示在辅助屏上
     */
    private fun hookActivityStartActivity() {
        val activity = Activity::class.java

        var context: Context? = null
        XposedBridge.hookAllMethods(
            activity,
            "attach",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    context = param!!.args[0] as Context
                }
            }
        )

        XposedBridge.hookAllMethods(
            activity,
            "startActivity",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (param != null) {
                        val intent = param.args[0] as Intent
                        if (context != null) {
                            val broadcastIntent = Intent("com.sunshine.freeform.start_activity")
                            broadcastIntent.setPackage("com.sunshine.freeform")
                            broadcastIntent.putExtra("packageName", intent.component?.packageName)
                            context!!.sendBroadcast(broadcastIntent)
                        }
                    }
                }
            }
        )
    }

    @SuppressLint("PrivateApi")
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        //只有10上需要
        if (android.os.Build.VERSION.SDK_INT == 29) {
            val activityThread =
                Class.forName("android.app.ActivityThread")
            XposedBridge.hookAllMethods(activityThread, "systemMain", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    hookTaskChangeNotificationController()
                }
            })
        }

    }

    /**
     * 不显示应用不支持辅助屏提示
     */
    private fun hookTaskChangeNotificationController() {
        val classLoader = Thread.currentThread().contextClassLoader
        val activityStackSupervisor = XposedHelpers.findClass(
            "com.android.server.wm.ActivityStackSupervisor",
            classLoader
        )
        XposedBridge.hookAllMethods(
            activityStackSupervisor,
            "handleNonResizableTaskIfNeeded",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    if (param != null) {
                        val taskRecordClazz = XposedHelpers.findClass("com.android.server.wm.TaskRecord", classLoader)
                        val mTaskField = taskRecordClazz.getDeclaredField("mStack")
                        mTaskField.isAccessible = true
                        val task = mTaskField.get(param.args[0])

                        val taskClazz = XposedHelpers.findClass("com.android.server.wm.ActivityStack", classLoader)
                        val mDisplayIdField = taskClazz.getDeclaredField("mDisplayId")
                        mDisplayIdField.isAccessible = true

                        val mDisplayId = mDisplayIdField.get(task) as Int
                        val preferredDisplayId = param.args[2] as Int
                        if (mDisplayId != preferredDisplayId) {
                            param.args[2] = mDisplayId
                        }
                    }
                }
            }
        )
    }
}