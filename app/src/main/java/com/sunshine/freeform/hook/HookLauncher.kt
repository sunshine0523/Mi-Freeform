package com.sunshine.freeform.hook

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.get
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.hook.utils.XLog
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

    //优化 多任务打开米窗的方式更改为点击应用图标后发现
    private fun hookLauncherAfterQ(classLoader: ClassLoader) {
        val taskMenuViewClass = XposedHelpers.findClass("com.android.quickstep.views.TaskMenuView", classLoader)

        XposedBridge.hookAllMethods(
            taskMenuViewClass,
            "addMenuOptions",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val taskMenuViewObj = param.thisObject as ViewGroup
                    val mOptionLayout = XposedHelpers.getObjectField(taskMenuViewObj, "mOptionLayout") as LinearLayout
                    val o = mOptionLayout[mOptionLayout.childCount - 1] as ViewGroup
                    val bg = o.background
                    val textColor = (o[1] as TextView).textColors
                    val menuOptionView = LayoutInflater.from(getUserContext()).inflate(R.layout.task_view_menu_option, taskMenuViewObj, false) as ViewGroup
                    menuOptionView.background = bg
                    menuOptionView[0].backgroundTintList = textColor
                    (menuOptionView[1] as TextView).setTextColor(textColor)
                    mOptionLayout.addView(menuOptionView)

                    addFreeformFunctionAfterQ(taskMenuViewObj, menuOptionView)
                }
            }
        )
    }

    private fun addFreeformFunctionAfterQ(paramObj: Any, menuOptionView: View) {
        try {
            val taskView = XposedHelpers.getObjectField(paramObj, "mTaskView") as FrameLayout
            val taskObj = XposedHelpers.callMethod(taskView, "getTask")
            val taskKeyObj = XposedHelpers.getObjectField(taskObj, "key")
            val userId = XposedHelpers.getIntField(taskKeyObj, "userId")
            val topComponent = XposedHelpers.callMethod(taskObj, "getTopComponent") as ComponentName

            menuOptionView.setOnClickListener {
                val intent = Intent("com.sunshine.freeform.start_freeform").apply {
                    setPackage("com.sunshine.freeform")
                    putExtra("packageName", topComponent.packageName)
                    putExtra("activityName", topComponent.className)
                    putExtra("userId", userId)
                }
                taskView.context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            XLog.d("HookLauncher $e")
        }
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
}