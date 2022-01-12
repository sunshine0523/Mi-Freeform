package com.sunshine.freeform.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.updatePadding
import com.sunshine.freeform.R
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author sunshine
 * @date 2021/3/3
 */
class HookStartActivity : IXposedHookZygoteInit, IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {

        hookTest(lpparam!!.classLoader)

        if (android.os.Build.VERSION.SDK_INT == 29) {
            //hookActivityStartActivity()
        }
    }

    private fun hookTest(classLoader: ClassLoader){
//        val decorViewClazz = XposedHelpers.findClass("com.android.internal.policy.DecorView", classLoader)
//
//        XposedBridge.hookAllConstructors(
//            decorViewClazz,
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//
//                    val resources = XposedHelpers.callMethod(param!!.thisObject, "getResources")
//                    val configuration = XposedHelpers.callMethod(resources, "getConfiguration")
//                    val winConfig = XposedHelpers.getObjectField(configuration, "windowConfiguration")
//                    XposedHelpers.callMethod(winConfig, "setWindowingMode", 5)
//                    val rect = Rect(100, 100, 1000, 1000)
//                    XposedHelpers.callMethod(winConfig, "setBounds", rect)
//                    XposedHelpers.callMethod(winConfig, "setAppBounds", rect)
//                }
//            }
//        )
//
//        XposedHelpers.findAndHookMethod(
//            decorViewClazz,
//            "createDecorCaptionView",
//            LayoutInflater::class.java,
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    super.beforeHookedMethod(param)
//
//                    val window = XposedHelpers.getObjectField(param!!.thisObject, "mWindow")
//                    val attrs = XposedHelpers.callMethod(window, "getAttributes") as WindowManager.LayoutParams
//
//                    val resources = XposedHelpers.callMethod(param.thisObject, "getResources")
//                    val configuration = XposedHelpers.callMethod(resources, "getConfiguration")
//                    val winConfig = XposedHelpers.getObjectField(configuration, "windowConfiguration")
////                    XposedHelpers.callMethod(winConfig, "setWindowingMode", 5)
////                    val rect = Rect(100, 100, 1000, 1000)
////                    XposedHelpers.callMethod(winConfig, "setBounds", rect)
////                    XposedHelpers.callMethod(winConfig, "setAppBounds", rect)
//
////
//                    XposedBridge.log(winConfig.toString())
//                }
//
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//                }
//            }
//        )
//
//        XposedHelpers.findAndHookMethod(
//            decorViewClazz,
//            "inflateDecorCaptionView",
//            LayoutInflater::class.java,
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    super.beforeHookedMethod(param)
//                }
//
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//
//                    XposedBridge.log(param!!.result.toString())
//                }
//            }
//        )

//        XposedHelpers.findAndHookMethod(
//            decorViewClazz,
//            "onResourcesLoaded",
//            LayoutInflater::class.java,
//            Int::class.javaPrimitiveType,
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
////                    val activityThread = Class.forName("android.app.ActivityThread")
////                    val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
////                    val application = activityThread.getMethod("getApplication").invoke(currentActivityThread) as Application
////                    val context = application.createPackageContext("com.sunshine.freeform", Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
////
////                    val mContentRootField = decorViewClazz.getDeclaredField("mContentRoot")
////                    mContentRootField.isAccessible = true
////                    val mContentRoot = mContentRootField.get(param!!.thisObject) as ViewGroup
////
////                    val view = LayoutInflater.from(context).inflate(R.layout.test, null, false)
////                    view.layoutParams = ViewGroup.LayoutParams(1000, 1500)
////
////                    val root: LinearLayout = view.findViewById(R.id.root)
////
////                    val decorView = (param.thisObject as ViewGroup)
////
////                    val configuration = decorView.resources.configuration
////
////                    XposedBridge.log(configuration::class.java.getField("windowConfiguration").get(configuration).toString())
////
////                    decorView.removeView(mContentRoot)
////
////                    root.addView(mContentRoot)
////
////                    decorView.addView(view)
////
////                    val mContentRootField = decorViewClazz.getDeclaredField("mContentRoot")
////                    mContentRootField.isAccessible = true
////                    val mContentRoot = mContentRootField.get(param!!.thisObject) as ViewGroup
////
////                    (param.thisObject as ViewGroup).removeView(mContentRoot)
////
////                    val root = view.findViewById(R.id.root) as LinearLayout
////                    root.addView(mContentRoot)
//
//
////                    (param.thisObject as ViewGroup).addView(view)
////
////                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
////                    //windowManager.removeView((param!!.thisObject as ViewGroup))
////                    windowManager.updateViewLayout((param!!.thisObject as ViewGroup), layoutParams)
//                }
//            }
//        )
//
//        XposedHelpers.findAndHookMethod(
//            decorViewClazz,
//            "updateDecorCaptionStatus",
//            Configuration::class.java,
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    XposedBridge.log("test:" + param!!.thisObject)
//
//                    val layoutParams = WindowManager.LayoutParams()
//                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//                    layoutParams.width = 100
//                    layoutParams.height = 200
//
//                    (param.thisObject as FrameLayout).layoutParams = layoutParams
////                    val activityThread = Class.forName("android.app.ActivityThread")
////                    val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
////                    val application = activityThread.getMethod("getApplication").invoke(currentActivityThread) as Application
////                    val context = application.createPackageContext("com.sunshine.freeform", Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
////
////                    val layoutParams = WindowManager.LayoutParams()
////                    layoutParams.apply {
////                        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
////                        x = 0
////                        y = 0
////                        this.width = 500
////                        this.height = 1000
////                        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
////                    }
//
////                    val view = LayoutInflater.from(context).inflate(R.layout.test, null, false)
////                    view.layoutParams = ViewGroup.LayoutParams(1000, 1500)
////
////                    val mContentRootField = decorViewClazz.getDeclaredField("mContentRoot")
////                    mContentRootField.isAccessible = true
////                    val mContentRoot = mContentRootField.get(param!!.thisObject) as ViewGroup
////
////                    (param.thisObject as ViewGroup).removeView(mContentRoot)
////
////                    val root = view.findViewById(R.id.root) as LinearLayout
////                    root.addView(mContentRoot)
//
//
////                    (param.thisObject as ViewGroup).addView(view)
////
////                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
////                    //windowManager.removeView((param!!.thisObject as ViewGroup))
////                    windowManager.updateViewLayout((param!!.thisObject as ViewGroup), layoutParams)
//                }
//            }
//        )
//
//        XposedHelpers.findAndHookMethod(
//            decorViewClazz,
//            "onLayout",
//            Boolean::class.javaPrimitiveType,
//            Int::class.javaPrimitiveType,
//            Int::class.javaPrimitiveType,
//            Int::class.javaPrimitiveType,
//            Int::class.javaPrimitiveType,
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    super.beforeHookedMethod(param)
//                    XposedBridge.log(param!!.args[0].toString())
//                    XposedBridge.log(param.args[1].toString())
//                    XposedBridge.log(param.args[2].toString())
//                    XposedBridge.log(param.args[3].toString())
//                    XposedBridge.log(param.args[4].toString())
//                }
//            }
//        )

        val activityThreadClazz = XposedHelpers.findClass("android.app.ActivityThread", classLoader)

//        XposedBridge.hookAllMethods(
//            activityThreadClazz,
//            "handleResumeActivity",
//            object : XC_MethodHook() {
//                override fun beforeHookedMethod(param: MethodHookParam?) {
//                    super.beforeHookedMethod(param)
//                    //XposedBridge.log("handleResumeActivity $param")
//                    val r = XposedHelpers.callMethod(param!!.thisObject, "performResumeActivity", param.args[0], param.args[1], param.args[3])
//                    if (r != null) {
//                        val activity = XposedHelpers.getObjectField(r, "activity")
//                        val window = XposedHelpers.callMethod(activity, "getWindow")
//                        val layoutParams = XposedHelpers.callMethod(window, "getAttributes") as WindowManager.LayoutParams
//                        val decor = XposedHelpers.callMethod(window, "getDecorView") as View
//                        val wm = XposedHelpers.callMethod(activity, "getWindowManager")
////                        layoutParams.width = 800
////                        layoutParams.height = 1200
////                        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
////                        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
//                        XposedBridge.log(layoutParams.toString())
//                        (wm as WindowManager).addView(decor, layoutParams)
//                    }
//                }
//
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    super.afterHookedMethod(param)
//                }
//            }
//        )
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
//                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                            val activityOptions = ActivityOptions.makeBasic()
//                            activityOptions.launchDisplayId = 1
                            val broadcastIntent = Intent("com.sunshine.freeform.start_activity")
                            broadcastIntent.setPackage("com.sunshine.freeform")
                            broadcastIntent.putExtra("packageName", intent.component?.packageName)
                            context!!.sendBroadcast(broadcastIntent)
//                            context!!.startActivity(intent, activityOptions.toBundle())
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

                        val mStackIdFiled = taskClazz.getDeclaredField("mStackId")
                        mStackIdFiled.isAccessible = true
                        val mStackId = mStackIdFiled.get(task)

                        val mDisplayId = mDisplayIdField.get(task) as Int
                        val preferredDisplayId = param.args[2] as Int
                        if (mDisplayId != preferredDisplayId) {
                            param.args[2] = mDisplayId
//                            XposedBridge.log("am display move-stack $mStackId $mDisplayId")
//                            Thread {
//                                XposedBridge.log(ShellForSystemUtils.execCommand("am stack list", false).successMsg)
//                                ShellForSystemUtils.execCommand("am display move-stack $mStackId $mDisplayId", false)
//                            }.start()

                        }
                    }
                }
            }
        )
    }
}