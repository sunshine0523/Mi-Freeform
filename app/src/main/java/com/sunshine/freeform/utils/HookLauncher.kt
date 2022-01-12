package com.sunshine.freeform.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author sunshine
 * @date 2021/3/8
 */
class HookLauncher : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {

        //通过包名判断是否是桌面
        if (lpparam?.packageName!! == "com.google.android.apps.nexuslauncher" || lpparam.packageName!! == "com.android.launcher3" || lpparam.packageName!! == "ch.deletescape.lawnchair.ci") {
            hookLauncher("com.android", lpparam.classLoader)
        }
        if (lpparam.packageName!! == "net.oneplus.launcher") {
            hookLauncher("net.oneplus", lpparam.classLoader)
        }
    }

    private fun hookLauncher(packageName: String, classLoader: ClassLoader) {
        val taskViewClazz = XposedHelpers.findClass("$packageName.quickstep.views.TaskView", classLoader)
        val taskMenuViewClazz = XposedHelpers.findClass("$packageName.quickstep.views.TaskMenuView", classLoader)

        XposedBridge.hookAllMethods(
            taskMenuViewClazz,
            "populateAndShowForTask",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if (param != null) {
                        val mOptionLayout = XposedHelpers.findField(taskMenuViewClazz, "mOptionLayout").get(param.thisObject) as LinearLayout
                        val mActivity = XposedHelpers.findField(taskMenuViewClazz, "mActivity").get(param.thisObject) as Context
                        val mTask = XposedHelpers.findField(taskViewClazz, "mTask").get(param.args[0])

                        val topActivity: ComponentName? =  mTask::class.java.getMethod("getTopComponent").invoke(mTask) as ComponentName

                        if (topActivity != null) {
                            val button = Button(mActivity)
                            val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            button.layoutParams = layoutParams
                            button.text = "米窗打开"
                            button.setBackgroundColor(Color.TRANSPARENT)

                            button.setOnClickListener {
                                val broadcastIntent = Intent("com.sunshine.freeform.start_by_mi_freeform")
                                broadcastIntent.setPackage("com.sunshine.freeform")
                                broadcastIntent.putExtra("packageName", topActivity.packageName)
                                broadcastIntent.putExtra("activityName", topActivity.flattenToString())
                                mActivity.sendBroadcast(broadcastIntent)
                            }
                            mOptionLayout.addView(button)
                        }
                    }
                }
            }
        )
    }
}